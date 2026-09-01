<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
* {
  box-sizing: border-box;
}
html,
body,
#app {
  margin: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* 移动端适配：常见手机尺寸（<=768px，覆盖 360~430px 主流机型） */
@media (max-width: 768px) {
  /* 弹窗自适应手机宽度，避免固定像素溢出 */
  .el-dialog {
    width: calc(100vw - 32px) !important;
    max-width: 560px;
    margin: 8vh auto 0;
  }
  .el-message-box {
    width: calc(100vw - 48px) !important;
  }
  .el-popper {
    max-width: calc(100vw - 24px);
  }
}
</style>
