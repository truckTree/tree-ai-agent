<template>
  <div class="chat-container">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <h1><span class="header-icon">&#x1F916;</span> AI 本地生活助手</h1>
      <span class="session-id">会话: {{ sessionId }}</span>
    </div>

    <!-- 聊天记录区域 -->
    <div class="chat-messages" ref="messagesRef">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        v-show="msg.visible !== false"
        :class="['message-wrapper', msg.role === 'user' ? 'user-wrapper' : 'ai-wrapper']"
      >
        <div v-if="msg.role === 'ai'" class="avatar ai-avatar">AI</div>
        <div :class="['message-bubble', msg.role]">
          <div :class="['message-content', { typing: msg.isTyping }]" v-html="renderContent(msg.displayed ?? msg.content)"></div>
        </div>
        <div v-if="msg.role === 'user'" class="avatar user-avatar">我</div>
      </div>
      <!-- 等待第一个消息时的加载指示器 -->
      <div v-if="loading" class="message-wrapper ai-wrapper">
        <div class="avatar ai-avatar">AI</div>
        <div class="message-bubble ai loading-bubble">
          <span class="dot"></span><span class="dot"></span><span class="dot"></span>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <div class="input-wrapper">
        <input
          v-model="inputMessage"
          type="text"
          placeholder="输入您的问题..."
          @keydown.enter="sendMessage"
          :disabled="loading"
          class="chat-input"
        />
        <button @click="sendMessage" :disabled="loading || !inputMessage.trim()" class="send-btn">
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'

const sessionId = ref('')
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messagesRef = ref(null)
let eventSource = null
let isTypingActive = false
const typewriterQueue = []
const TYPEWRITER_DURATION = 5000 // 打字机总时长（毫秒）
const MIN_TYPEWRITER_LENGTH = 50 // 少于该字数不打字，直接显示

// 打字机效果：逐字显示内容
function startTypewriter(index) {
  const msg = messages.value[index]
  if (!msg) return
  const fullContent = msg.content

  // 如果已有打字机在运行，加入队列等待（所有消息都排队）
  if (isTypingActive) {
    typewriterQueue.push(index)
    return
  }

  // 显示气泡
  msg.visible = true

  // 少于 50 个字，直接全部显示，不进队列
  if (fullContent.length < MIN_TYPEWRITER_LENGTH) {
    msg.displayed = fullContent
    msg.isTyping = false
    processQueue()
    return
  }

  // 开始打字机效果
  isTypingActive = true
  msg.displayed = ''
  msg.isTyping = true
  let charIndex = 0
  const speed = Math.max(10, Math.floor(TYPEWRITER_DURATION / fullContent.length))
  const timer = setInterval(() => {
    if (charIndex < fullContent.length) {
      msg.displayed += fullContent[charIndex]
      charIndex++
      scrollToBottom()
    } else {
      msg.isTyping = false
      isTypingActive = false
      clearInterval(timer)
      processQueue()
    }
  }, speed)
}

function processQueue() {
  if (typewriterQueue.length > 0) {
    const nextIndex = typewriterQueue.shift()
    startTypewriter(nextIndex)
  }
}

// 生成会话 ID
function generateSessionId() {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8)
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

// 将文本中的换行转为 <br>，保留空格
function renderContent(text) {
  if (!text) return ''
  return text.replace(/\n/g, '<br>')
}

// 发送消息
function sendMessage() {
  const msg = inputMessage.value.trim()
  if (!msg || loading.value) return

  // 添加用户消息
  messages.value.push({ role: 'user', content: msg })
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()

  // 使用 EventSource 连接 SSE
  const url = `/api/ai/manus/chat?message=${encodeURIComponent(msg)}&messageId=${encodeURIComponent(sessionId.value)}`
  eventSource = new EventSource(url)

  eventSource.onmessage = (event) => {
    let content = event.data
    try {
      const data = JSON.parse(event.data)
      if (data.content) {
        content = data.content
      }
    } catch {
      // 非 JSON 格式，直接使用原始文本
    }
    // 首个消息到达，关闭加载指示器
    loading.value = false
    // 每个 SSE 事件生成一个独立的气泡，并启动打字机效果
    messages.value.push({ role: 'ai', content, displayed: '', visible: false })
    startTypewriter(messages.value.length - 1)
    scrollToBottom()
  }

  eventSource.onerror = () => {
    eventSource.close()
    loading.value = false
    scrollToBottom()
  }
}

// 监听 messages 变化自动滚动
watch(messages, () => {
  scrollToBottom()
}, { deep: true })

// 初始化
onMounted(() => {
  sessionId.value = generateSessionId()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(135deg, #e8f4fd 0%, #f0f8ff 50%, #e8f4fd 100%);
}

/* 头部 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #ffffff;
  border-bottom: 1px solid #d0e8f5;
  box-shadow: 0 2px 8px rgba(0, 120, 200, 0.08);
}

.chat-header h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1a7ab5;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 24px;
}

.session-id {
  font-size: 12px;
  color: #8ab4d0;
  background: #e8f4fd;
  padding: 4px 12px;
  border-radius: 12px;
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #c0dcec;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

/* 消息包装 */
.message-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 80%;
}

.user-wrapper {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.ai-wrapper {
  align-self: flex-start;
}

/* 头像 */
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.ai-avatar {
  background: #1a7ab5;
  color: #ffffff;
}

.user-avatar {
  background: #b3d9f2;
  color: #1a5a7a;
}

/* 消息气泡 */
.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  max-width: 100%;
}

.message-bubble.user {
  background: #1a7ab5;
  color: #ffffff;
  border-bottom-right-radius: 4px;
}

.message-bubble.ai {
  background: #ffffff;
  color: #333333;
  border: 1px solid #d0e8f5;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 120, 200, 0.06);
}

.message-content {
  white-space: pre-wrap;
}

.message-content.typing::after {
  content: '|';
  animation: blink 0.8s infinite;
  color: #1a7ab5;
  font-weight: bold;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 加载指示器（三点动画） */
.loading-bubble {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 14px 20px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #8ab4d0;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}

.dot:nth-child(2) {
  animation-delay: -0.16s;
}

.dot:nth-child(3) {
  animation-delay: 0s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 输入区域 */
.chat-input-area {
  padding: 16px 24px 20px;
  background: #ffffff;
  border-top: 1px solid #d0e8f5;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  max-width: 900px;
  margin: 0 auto;
  align-items: center;
}

.chat-input {
  flex: 1;
  padding: 12px 18px;
  border: 2px solid #d0e8f5;
  border-radius: 24px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  background: #f5faff;
}

.chat-input:focus {
  border-color: #1a7ab5;
  background: #ffffff;
}

.chat-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.send-btn {
  padding: 12px 28px;
  background: #1a7ab5;
  color: #ffffff;
  border: none;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
  white-space: nowrap;
}

.send-btn:hover:not(:disabled) {
  background: #15689e;
}

.send-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>