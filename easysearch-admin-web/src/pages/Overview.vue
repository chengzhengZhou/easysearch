<script setup lang="ts"></script>

<template>
  <header class="qpTopbar">
    <strong>总览</strong>
    <span class="qpSub">资源与版本概览（待完善）</span>
  </header>
  <section class="qpPad">
    <div class="panel">
      <p class="hint">总览页待实现：各模块规则量、当前发布版本、最近操作等。</p>
    </div>
  </section>
</template>

<style scoped>
.qpTopbar {
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid var(--border);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.qpSub {
  font-size: 13px;
  color: var(--muted);
}
.qpPad {
  padding: 16px;
}
.panel {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px;
}
</style>

