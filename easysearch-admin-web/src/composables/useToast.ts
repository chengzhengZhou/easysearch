import { ref } from 'vue'

/**
 * Toast 消息提示组合式函数
 * 用于显示临时消息提示
 */
export function useToast(defaultDuration = 3000) {
  const message = ref<string>('')

  function show(msg: string, duration = defaultDuration) {
    message.value = msg
    if (duration > 0) {
      setTimeout(() => {
        if (message.value === msg) {
          message.value = ''
        }
      }, duration)
    }
  }

  function clear() {
    message.value = ''
  }

  /**
   * 从错误对象中提取消息并显示
   */
  function showError(e: any, fallback = '操作失败') {
    const msg = e?.response?.data?.message ?? e?.message ?? fallback
    show(msg)
  }

  return {
    message,
    show,
    clear,
    showError,
  }
}
