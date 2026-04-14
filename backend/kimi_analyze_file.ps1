# Kimi AI - Analyze Already Uploaded File
# Based on official API documentation
# Usage: .\kimi_analyze_file.ps1 "file-ID" ["Custom Prompt"]

param(
    [Parameter(Position=0, Mandatory=$true)]
    [string]$FileID,
    
    [Parameter(Position=1)]
    [string]$SystemPrompt = "You are a helpful expert document analyzer",
    
    [Parameter(Position=2)]
    [string]$UserPrompt = "Please analyze this document and provide a comprehensive summary",
    
    [Parameter(Position=3)]
    [string]$Model = "moonshot-v1-8k"
)

$API_KEY = $env:KIMI_API_KEY
$FILES_URL = "https://api.moonshot.cn/v1/files"
$CHAT_URL = "https://api.moonshot.cn/v1/chat/completions"

function Write-Success { param([string]$Msg) Write-Host "✓ $Msg" -ForegroundColor Green }
function Write-Error-Custom { param([string]$Msg) Write-Host "✗ $Msg" -ForegroundColor Red }
function Write-Info { param([string]$Msg) Write-Host "ℹ $Msg" -ForegroundColor Cyan }
function Write-Section { param([string]$Msg) Write-Host "`n════ $Msg ════" -ForegroundColor Magenta }

# Check API key
if (-not $API_KEY) {
    Write-Error-Custom "API Key not set"
    exit 1
}

Write-Section "Retrieving File Content"

try {
    $handler = New-Object System.Net.Http.HttpClientHandler
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $API_KEY)
    
    $contentUrl = "$FILES_URL/$FileID/content"
    Write-Info "Getting content from: $FileID"
    
    $response = $client.GetAsync($contentUrl).Result
    
    if (-not $response.IsSuccessStatusCode) {
        Write-Error-Custom "Failed to get content: $($response.StatusCode)"
        $client.Dispose()
        exit 1
    }
    
    $fileContent = $response.Content.ReadAsStringAsync().Result
    Write-Success "Content retrieved ($($fileContent.Length) chars)"
    $client.Dispose()
    
} catch {
    Write-Error-Custom "Error: $_"
    exit 1
}

Write-Section "Analyzing with AI"

try {
    $messages = @(
        @{ role = "system"; content = $SystemPrompt },
        @{ role = "system"; content = $fileContent },
        @{ role = "user"; content = $UserPrompt }
    )
    
    $payload = @{
        model = $Model
        messages = $messages
    } | ConvertTo-Json -Depth 10
    
    $handler = New-Object System.Net.Http.HttpClientHandler
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $API_KEY)
    
    $content = New-Object System.Net.Http.StringContent($payload, [System.Text.Encoding]::UTF8, "application/json")
    $response = $client.PostAsync($CHAT_URL, $content).Result
    
    if (-not $response.IsSuccessStatusCode) {
        Write-Error-Custom "Analysis failed: $($response.StatusCode)"
        $client.Dispose()
        exit 1
    }
    
    $responseBody = $response.Content.ReadAsStringAsync().Result
    $responseJson = $responseBody | ConvertFrom-Json
    
    Write-Success "Analysis complete"
    $client.Dispose()
    
} catch {
    Write-Error-Custom "Error: $_"
    exit 1
}

Write-Section "Results"

if ($responseJson.choices -and $responseJson.choices.Count -gt 0) {
    Write-Host $responseJson.choices[0].message.content -ForegroundColor White
} else {
    Write-Error-Custom "No response"
    exit 1
}

Write-Section "Token Usage"
Write-Info "Prompt: $($responseJson.usage.prompt_tokens)"
Write-Info "Completion: $($responseJson.usage.completion_tokens)"
Write-Success "Total: $($responseJson.usage.total_tokens)"

# Save
$outputFile = "$env:TEMP\kimi_analysis_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
@"
File ID: $FileID
Tokens: $($responseJson.usage.total_tokens)
Date: $(Get-Date)

$($responseJson.choices[0].message.content)
"@ | Out-File $outputFile -Encoding UTF8

Write-Info "Saved to: $outputFile"
