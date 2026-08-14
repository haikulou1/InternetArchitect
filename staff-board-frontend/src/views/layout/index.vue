<template>
  <el-container style="height: 100vh;">
    <el-aside width="220px" style="background: #304156;">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold;">
        人员看板系统
      </div>
      <el-menu :default-active="route.path" background-color="#304156" text-color="#bfcbd9"
        active-text-color="#409EFF" router>
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>看板总览</span>
        </el-menu-item>
        <el-menu-item index="/employees">
          <el-icon><User /></el-icon>
          <span>员工列表</span>
        </el-menu-item>
        <el-menu-item index="/employees/card">
          <el-icon><Grid /></el-icon>
          <span>卡片视图</span>
        </el-menu-item>
        <el-menu-item index="/import" v-if="userStore.isAdmin">
          <el-icon><Upload /></el-icon>
          <span>批量导入</span>
        </el-menu-item>
        <el-menu-item index="/budget">
          <el-icon><Money /></el-icon>
          <span>成本预算</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display: flex; align-items: center; justify-content: flex-end; border-bottom: 1px solid #eee;">
        <span style="margin-right: 16px;">{{ userStore.username }}</span>
        <el-button type="danger" text @click="handleLogout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>
