<template>
  <RouterView v-if="route.meta.public" />
  <MainLayout v-else />
</template>

<script setup>
import { useRoute, RouterView } from 'vue-router'
import MainLayout from './layout/MainLayout.vue'

const route = useRoute()
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  background: #f3f6fb;
}
</style>
