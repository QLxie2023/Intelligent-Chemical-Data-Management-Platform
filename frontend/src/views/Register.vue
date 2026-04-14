<template>
    <div class="min-h-screen flex items-center justify-center bg-gray-100">

          <!-- background -->
      <div
        class="absolute inset-0 bg-center bg-cover bg-no-repeat opacity-80 pointer-events-none"
        :style="{ backgroundImage: `url(${bgImage})` }"
      ></div>

      <div
        class="absolute inset-0 backdrop-blur-md bg-white/10 pointer-events-none"
      ></div>

      <div class="relative z-10 bg-white/30 p-8 rounded-xl shadow-md w-full max-w-md">

        <div class="flex justify-center mb-4">
          <img
            src="../assets/logo.png"
            alt="Chem+ Logo"
            class="w-24 h-24 rounded-full object-cover shadow"
          />
        </div>

        <h2 class="text-2xl font-bold mb-6 text-center">用户注册</h2>
  
        <div class="space-y-4">
          <input
            v-model="form.username"
            type="text"
            placeholder="Username"
            class="w-full px-4 py-2 rounded-lg border focus:outline-none focus:ring"
          />
  
          <input
            v-model="form.password"
            type="Password"
            placeholder="Password(At least 6 bits)"
            class="w-full px-4 py-2 rounded-lg border focus:outline-none focus:ring"
          />
  
          <input
            v-model="form.invitationCode"
            type="text"
            placeholder="invitation code"
            class="w-full px-4 py-2 rounded-lg border focus:outline-none focus:ring"
          />
  
          <button
            @click="handleRegister"
            class="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition"
          >
            register
          </button>
  
          <p class="text-sm text-center mt-3">
            have an account?
            <router-link to="/login" class="text-blue-600">Log in immediately</router-link>
          </p>
        </div>
  
        <p v-if="errorMsg" class="text-red-600 text-center mt-4">{{ errorMsg }}</p>
        <p v-if="successMsg" class="text-green-600 text-center mt-4">{{ successMsg }}</p>
      </div>
    </div>
  </template>
  
  <script setup>
  import { reactive, ref } from "vue";
  import { useRouter } from "vue-router";
  import request from "../utils/request";
  
  import bgImage from "../assets/eb5f0a379a83260d0b8356a84e280f69.jpg";

  const router = useRouter();
  const form = reactive({
    username: "",
    password: "",
    invitationCode: ""
  });
  
  const errorMsg = ref("");
  const successMsg = ref("");
  
  const handleRegister = async () => {
    errorMsg.value = "";
    successMsg.value = "";
  
    if (!form.username || !form.password || !form.invitationCode) {
      errorMsg.value = "Please complete all the fields completely.";
      return;
    }
    if (form.password.length < 6) {
      errorMsg.value = "The password must be at least 6 characters long.";
      return;
    }
  
    try {
      const res = await request.post("/auth/register", form);
  
      if (res.code === 200) {
        successMsg.value = "Registration successful. Redirecting to login...";
        setTimeout(() => router.push("/login"), 1200);
      } 
      else if (res.code === 409) {
      errorMsg.value = "The username is already in use and cannot be registered.";
      }
      else if (res.code === 400) {
        errorMsg.value = "The invitation code is invalid or has already been used.";
      } else {
        errorMsg.value = res.message || "fail to register";
      }
    } catch (err) {
      errorMsg.value = err.message || "Registration failed. Please check your input.";
    }
  };
  </script>
  