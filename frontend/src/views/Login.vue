<template>
  <div class="min-h-screen relative flex items-center justify-center overflow-hidden">

    <!-- background -->
    <div
      class="absolute inset-0 bg-center bg-cover bg-no-repeat opacity-80 pointer-events-none"
      :style="{ backgroundImage: `url(${bgImage})` }"
    ></div>

    <div
      class="absolute inset-0 backdrop-blur-md bg-white/10 pointer-events-none"
    ></div>

    <!-- login -->
    <div class="relative z-10 bg-white/30 p-8 rounded-xl shadow-xl w-full max-w-md">

      <!-- Logo -->
      <div class="flex justify-center mb-4">
        <img
          src="../assets/logo.png"
          alt="Chem+ Logo"
          class="w-24 h-24 rounded-full object-cover shadow"
        />
      </div>

      <h2 class="text-2xl font-bold mb-6 text-center">User login</h2>

      <div class="space-y-4">
        <input
          v-model="form.username"
          type="text"
          placeholder="Username"
          class="w-full px-4 py-2 rounded-lg border focus:outline-none focus:ring"
        />

        <input
          v-model="form.password"
          type="password"
          placeholder="Password"
          class="w-full px-4 py-2 rounded-lg border focus:outline-none focus:ring"
        />

        <button
          @click="handleLogin"
          class="w-full bg-green-600 text-white py-2 rounded-lg hover:bg-green-700 transition"
        >
          Login
        </button>

        <p class="text-sm text-center mt-3">
          No account?
          <router-link to="/register" class="text-blue-600">
            Register now
          </router-link>
        </p>
      </div>

      <p v-if="errorMsg" class="text-red-600 text-center mt-4">
        {{ errorMsg }}
      </p>
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
  password: ""
});

const errorMsg = ref("");

const handleLogin = async () => {
  errorMsg.value = "";

  if (!form.username || !form.password) {
    errorMsg.value = "Please enter your username and password";
    return;
  }

  try {
    const res = await request.post("/auth/login", form);

    if (res.code === 200) {
      const { token, user } = res.data;
      localStorage.setItem("token", token);
      localStorage.setItem("user", JSON.stringify(user));
      router.push("/");
    } else if (res.code === 404) {
      errorMsg.value = "User does not exist!";
    } else if (res.code === 401) {
      errorMsg.value = "Password is incorrect!";
    } else {
      errorMsg.value = res.message || "Login failed.";
    }
  } catch (err) {
    errorMsg.value = err.message || "Login failed.";
  }
};
</script>
