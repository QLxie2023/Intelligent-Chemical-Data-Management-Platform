# Kimi AI Official API - Complete Workflow
# Upload file -> Parse -> Analyze -> Return results
# PowerShell 5.1 Compatible

param(
    [Parameter(Position=0, Mandatory=$true)]
    [string]$FilePath,
    
    [Parameter(Position=1)]
    [string]$SystemPrompt = "You are a professional materials science data assistant specializing in thermal spray coatings. Please analyze the uploaded paper carefully and extract experimental data in the specified Markdown table format, paying special attention to tables, figures, and numerical data.",
    
    [Parameter(Position=2)]
    [string]$UserPrompt = "Please act as a professional materials science data assistant. Read the paper I uploaded about thermal spray Al2O3-TiO2 or Cr2O3-TiO2 coatings. Your task is to extract experimental data from the paper and organize it into Markdown table format. Focus on the following fields (fill 'N/A' if not mentioned; use average or range format if the paper shows range values): 1. Material_Composition (coating composition, e.g. Al2O3-13wt%TiO2) 2. Spray_Process (spray technology, e.g. APS, HVOF) 3. Spray_Distance_mm (spray distance) 4. Gun_Speed_mm_s (gun speed) 5. Powder_Feed_Rate_g_min (powder feed rate) 6. Porosity_% (porosity) 7. Microhardness_GPa (hardness, note if HV) 8. Counterpart_Ball (counterpart ball material, e.g. Steel, Si3N4) 9. Load_N (load) 10. Wear_Rate (wear rate, unify units and note original units in remarks). Note: If the paper contains comparisons of different parameters (e.g. different TiO2 content or different loads), extract each group of data as one row in the table. Focus on checking the 'Table' sections in the paper. If data cannot be read from images, tell me which figure contains the relevant data.",
    
    [Parameter(Position=3)]
    [string]$Model = "moonshot-v1-128k"
)

# Configuration
$API_KEY = $env:KIMI_API_KEY
$FILES_URL = "https://api.moonshot.cn/v1/files"
$CHAT_URL = "https://api.moonshot.cn/v1/chat/completions"

# Helper functions - ASCII only
function WriteOK { Write-Host "[OK] $args" -ForegroundColor Green }
function WriteError { Write-Host "[ERROR] $args" -ForegroundColor Red }
function WriteInfo { Write-Host "[INFO] $args" -ForegroundColor Cyan }
function WriteWarn { Write-Host "[WARN] $args" -ForegroundColor Yellow }
function WriteHeader { Write-Host "" ; Write-Host "===== $args =====" -ForegroundColor Magenta }

# Step 1: Check API Key
WriteHeader "Configuration Check"

if (-not $API_KEY) {
    WriteError "API Key NOT set"
    WriteWarn "Run: `$env:KIMI_API_KEY = 'sk-YOUR_KEY'"
    exit 1
}

WriteOK "API Key set"
WriteOK "Files API: $FILES_URL"
WriteOK "Chat API: $CHAT_URL"
WriteInfo "Model: $Model"

# Step 2: Validate File
WriteHeader "File Validation"

if (-not (Test-Path $FilePath)) {
    WriteError "File not found: $FilePath"
    exit 1
}

$file = Get-Item $FilePath
$fileSizeMB = [math]::Round($file.Length / 1MB, 2)

WriteOK "File: $($file.Name)"
WriteOK "Size: $fileSizeMB MB"

if ($file.Length -gt 100MB) {
    WriteError "File exceeds 100MB limit"
    exit 1
}

# Step 3: Upload File
WriteHeader "Uploading File"

try {
    [System.Reflection.Assembly]::LoadWithPartialName("System.Net.Http") | Out-Null
    
    $fileBytes = [System.IO.File]::ReadAllBytes($FilePath)
    $fileName = [System.IO.Path]::GetFileName($FilePath)
    
    WriteInfo "Uploading $fileName..."
    
    # 创建临时文件用于上传
    $tempFile = New-TemporaryFile
    [System.IO.File]::WriteAllBytes($tempFile.FullName, $fileBytes)
    
    $handler = New-Object System.Net.Http.HttpClientHandler
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $API_KEY)
    $client.Timeout = [System.TimeSpan]::FromSeconds(60)
    
    $content = New-Object System.Net.Http.MultipartFormDataContent
    $fileStream = [System.IO.File]::OpenRead($tempFile.FullName)
    $fileContent = New-Object System.Net.Http.StreamContent($fileStream)
    $fileContent.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream")
    
    $content.Add($fileContent, "file", $fileName)
    
    $purposeContent = New-Object System.Net.Http.StringContent("file-extract")
    $content.Add($purposeContent, "purpose")
    
    $response = $client.PostAsync($FILES_URL, $content).Result
    
    if (-not $response.IsSuccessStatusCode) {
        WriteError "Upload failed: $($response.StatusCode)"
        WriteError $response.Content.ReadAsStringAsync().Result
        $fileStream.Dispose()
        $client.Dispose()
        Remove-Item $tempFile.FullName -Force
        exit 1
    }
    
    $responseBody = $response.Content.ReadAsStringAsync().Result
    $responseJson = $responseBody | ConvertFrom-Json
    
    WriteOK "Upload successful (HTTP 200)"
    
    if (-not $responseJson.id) {
        WriteError "No file ID in response"
        WriteError $responseBody
        $fileStream.Dispose()
        $client.Dispose()
        Remove-Item $tempFile.FullName -Force
        exit 1
    }
    
    $FileID = $responseJson.id
    WriteOK "File ID: $FileID"
    WriteOK "Status: $($responseJson.status)"
    WriteInfo "File bytes: $($responseJson.bytes)"
    
    $fileStream.Dispose()
    $client.Dispose()
    Remove-Item $tempFile.FullName -Force
    
} catch {
    WriteError "Upload error: $_"
    exit 1
}

# Step 4: Wait for Processing
WriteHeader "Waiting for File Processing"

$maxRetries = 30
$retryCount = 0
$statusOk = $false

while ($retryCount -lt $maxRetries) {
    Start-Sleep -Seconds 2
    $retryCount++
    
    try {
        $handler = New-Object System.Net.Http.HttpClientHandler
        $client = New-Object System.Net.Http.HttpClient($handler)
        $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $API_KEY)
        
        $statusUrl = "$FILES_URL/$FileID"
        $statusResponse = $client.GetAsync($statusUrl).Result
        
        if ($statusResponse.IsSuccessStatusCode) {
            $statusBody = $statusResponse.Content.ReadAsStringAsync().Result
            $statusJson = $statusBody | ConvertFrom-Json
            
            WriteInfo "Attempt $retryCount - Status: $($statusJson.status)"
            
            if ($statusJson.status -eq "ok") {
                WriteOK "File processing complete"
                $statusOk = $true
                $client.Dispose()
                break
            } elseif ($statusJson.status -eq "error") {
                WriteError "File processing error: $($statusJson.status_details)"
                $client.Dispose()
                exit 1
            }
        }
        
        $client.Dispose()
        
    } catch {
        WriteWarn "Status check error, will retry: $_"
    }
}

if (-not $statusOk) {
    WriteError "File processing timeout"
    exit 1
}

# Step 5: Get File Content
WriteHeader "Retrieving File Content"

try {
    $handler = New-Object System.Net.Http.HttpClientHandler
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $API_KEY)
    
    $contentUrl = "$FILES_URL/$FileID/content"
    WriteInfo "Fetching: $contentUrl"
    
    $contentResponse = $client.GetAsync($contentUrl).Result
    
    if (-not $contentResponse.IsSuccessStatusCode) {
        WriteError "Get content failed: $($contentResponse.StatusCode)"
        WriteError $contentResponse.Content.ReadAsStringAsync().Result
        exit 1
    }
    
    $fileContentText = $contentResponse.Content.ReadAsStringAsync().Result
    WriteOK "File content retrieved"
    WriteInfo "Content length: $($fileContentText.Length) characters"
    
    $client.Dispose()
    
} catch {
    WriteError "Get content error: $_"
    exit 1
}

# Step 6: Send Chat Request
WriteHeader "Analyzing with AI"

try {
    $messages = @(
        @{ role = "system"; content = $SystemPrompt },
        @{ role = "system"; content = $fileContentText },
        @{ role = "user"; content = $UserPrompt }
    )
    
    $payload = @{
        model = $Model
        messages = $messages
    } | ConvertTo-Json -Depth 10
    
    WriteInfo "Payload size: $($payload.Length) characters"
    WriteInfo "Message count: $($messages.Count)"
    
    Write-Host ""
    Write-Host "=== REQUEST PAYLOAD PREVIEW (first 300 chars) ===" -ForegroundColor DarkGray
    $preview = if ($payload.Length -gt 300) { $payload.Substring(0, 300) + "`n..." } else { $payload }
    Write-Host $preview -ForegroundColor Gray
    Write-Host "===============================================" -ForegroundColor DarkGray
    Write-Host ""
    
    WriteInfo "Sending analysis request..."
    
    $handler = New-Object System.Net.Http.HttpClientHandler
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $API_KEY)
    $client.DefaultRequestHeaders.Add("User-Agent", "PowerShell/5.1")
    $client.Timeout = [System.TimeSpan]::FromSeconds(60)
    
    $content = New-Object System.Net.Http.StringContent($payload, [System.Text.Encoding]::UTF8, "application/json")
    $chatResponse = $client.PostAsync($CHAT_URL, $content).Result
    
    if (-not $chatResponse.IsSuccessStatusCode) {
        WriteError "Analysis failed: $($chatResponse.StatusCode)"
        WriteError $chatResponse.Content.ReadAsStringAsync().Result
        exit 1
    }
    
    $responseBody = $chatResponse.Content.ReadAsStringAsync().Result
    $responseJson = $responseBody | ConvertFrom-Json
    
    WriteOK "Analysis successful (HTTP 200)"
    $client.Dispose()
    
} catch {
    WriteError "Analysis error: $_"
    exit 1
}

# Step 7: Display Results
WriteHeader "AI Analysis Results"

if ($responseJson.choices -and $responseJson.choices.Count -gt 0) {
    $result = $responseJson.choices[0].message.content
    Write-Host ""
    Write-Host $result
    Write-Host ""
} else {
    WriteError "No response content from AI"
    exit 1
}

# Step 8: Token Usage
WriteHeader "Token Usage"

if ($responseJson.usage) {
    WriteInfo "Prompt tokens: $($responseJson.usage.prompt_tokens)"
    WriteInfo "Completion tokens: $($responseJson.usage.completion_tokens)"
    WriteOK "Total tokens: $($responseJson.usage.total_tokens)"
}

# Step 9: Save Results
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outputFile = "$env:TEMP\kimi_analysis_$timestamp.txt"

$reportContent = @"
KIMI AI ANALYSIS REPORT
Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
File: $($file.Name)
File ID: $FileID
File Size: $fileSizeMB MB

--- SYSTEM PROMPT ---
$SystemPrompt

--- USER PROMPT ---
$UserPrompt

--- AI ANALYSIS RESULT ---
$result

--- TOKEN USAGE ---
Prompt: $($responseJson.usage.prompt_tokens)
Completion: $($responseJson.usage.completion_tokens)
Total: $($responseJson.usage.total_tokens)

--- MODEL INFO ---
Model: $($responseJson.model)
API: Files + Chat Completions
"@

$reportContent | Out-File $outputFile -Encoding UTF8

WriteHeader "Completion"
WriteOK "Analysis complete"
WriteInfo "Results saved: $outputFile"
WriteOK "File ID for reuse: $FileID"