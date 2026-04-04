<script setup lang="ts">
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <div class="qpApp">
    <aside class="qpSidebar">
      <div class="qpBrand">QP Admin</div>
      <div class="qpNavTitle">资源治理</div>
      <RouterLink class="qpNavItem" to="/overview" :class="{ active: route.name === 'overview' }">总览</RouterLink>
      <RouterLink class="qpNavItem" to="/intervention" :class="{ active: route.name === 'intervention' }">干预规则</RouterLink>
      <RouterLink class="qpNavItem" to="/synonym" :class="{ active: route.name === 'synonym' }">同义词</RouterLink>
      <RouterLink class="qpNavItem" to="/entity" :class="{ active: route.name === 'entity' }">实体词典</RouterLink>
      <RouterLink class="qpNavItem" to="/token" :class="{ active: route.name === 'token' }">分词词典</RouterLink>

      <div class="qpNavTitle">发布治理</div>
      <RouterLink class="qpNavItem" to="/publish" :class="{ active: route.name === 'publish' }">发布记录</RouterLink>
      <RouterLink class="qpNavItem" to="/audit" :class="{ active: route.name === 'audit' }">操作审计</RouterLink>
    </aside>

    <div class="qpMain">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.qpApp {
  display: flex;
  min-height: 100vh;
}
.qpSidebar {
  width: 220px;
  background: #101827;
  color: #c9d2e3;
  padding: 16px 12px;
  flex-shrink: 0;
}
.qpBrand {
  color: #fff;
  font-weight: 700;
  margin-bottom: 14px;
}
.qpNavTitle {
  color: #8ea0bc;
  font-size: 12px;
  margin: 10px;
  text-transform: uppercase;
}
.qpNavItem {
  display: block;
  padding: 9px 10px;
  border-radius: 8px;
  margin-bottom: 4px;
  color: inherit;
  text-decoration: none;
  font-size: 14px;
}
.qpNavItem.active {
  background: #1e2b44;
  color: #fff;
}
.qpMain {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
</style>

