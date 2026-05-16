<template>
  <div class="assistant-page">
    <div class="toolbar">
      <div>
        <h2>Smart Knowledge Q&A Assistant</h2>
        <span>Connects to DeepSeek and supports natural language queries for inventory, sales, payroll, and profit</span>
      </div>
      <div class="actions">
        <el-button class="outline-btn" @click="newConversation">New Chat</el-button>
        <el-button class="outline-btn danger" @click="clearHistory">Clear History</el-button>
      </div>
    </div>

    <div class="assistant-layout">
      <section class="panel chat-panel">
        <div class="messages">
          <div v-for="item in messages" :key="item.id" :class="['message', item.role]">
            <div class="bubble">{{ item.content }}</div>
          </div>
        </div>
        <div class="composer">
          <el-input v-model="question" type="textarea" :rows="3" placeholder="Example: Why did profit drop this month? Which products need replenishment?" @keydown="handleQuestionKeydown" />
          <el-button type="primary" :loading="loading" @click="ask">Send</el-button>
        </div>
      </section>

      <aside class="panel side-panel">
        <div class="panel-title">Quick Questions</div>
        <el-button v-for="item in quickQuestions" :key="item" plain @click="sendQuick(item)">
          {{ item }}
        </el-button>
        <el-alert class="tip" title="Business questions are routed to read-only SQL first; general questions are sent directly to DeepSeek." type="info" :closable="false" />
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const HISTORY_KEY = 'kfc_assistant_messages'
const createInitialMessages = () => [
  { id: Date.now(), role: 'assistant', content: 'Enter a business question. I will analyze it using current inventory, sales, payroll, profit, and waste data.' }
]
const loadMessages = () => {
  try {
    const stored = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
    return Array.isArray(stored) && stored.length ? stored : createInitialMessages()
  } catch {
    return createInitialMessages()
  }
}
const loading = ref(false)
const question = ref('')
const messages = ref(loadMessages())
const quickQuestions = [
  'How is profit this month?',
  'Which products should be replenished first?',
  'What may have caused the sales decline?',
  'Are there anomalies in employee performance or payroll?'
]

watch(messages, value => {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(value))
}, { deep: true })

const ask = async () => {
  if (loading.value) return
  const text = question.value.trim()
  if (!text) return ElMessage.warning('Please enter a question')
  messages.value.push({ id: Date.now(), role: 'user', content: text })
  question.value = ''
  loading.value = true
  try {
    const data = await request.post('/assistant/chat', { question: text })
    messages.value.push({ id: Date.now() + 1, role: 'assistant', content: formatAnswer(data) })
  } finally {
    loading.value = false
  }
}

const newConversation = () => {
  question.value = ''
  messages.value = createInitialMessages()
  ElMessage.success('New chat started')
}

const clearHistory = async () => {
  await ElMessageBox.confirm('Clear all local Q&A history?', 'Clear History', { type: 'warning' })
  question.value = ''
  messages.value = createInitialMessages()
  localStorage.removeItem(HISTORY_KEY)
  ElMessage.success('History cleared')
}

const handleQuestionKeydown = (event) => {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return
  event.preventDefault()
  ask()
}

const formatAnswer = (data) => {
  if (data?.sql) {
    console.info('[KFC Assistant SQL]', {
      sql: data.sql,
      rowCount: data.rowCount ?? 0
    })
  }
  return stripSqlFromAnswer(data?.answer || '')
}

const stripSqlFromAnswer = (answer) => {
  return String(answer)
    .replace(/\n{0,2}(SQL|Executed SQL):[\s\S]*?(?=\n{2,}(Rows|Result|Conclusion|Answer|$)|$)/gi, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

const sendQuick = (text) => {
  question.value = text
  ask()
}
</script>

<style scoped>
.assistant-page {
  max-width: 1300px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.toolbar h2 {
  color: #111827;
  letter-spacing: 0;
}

.toolbar span {
  color: #64748b;
  font-size: 14px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.outline-btn {
  border-color: #e4002b;
  color: #e4002b;
  background: #fff;
  border-radius: 6px;
}

.outline-btn:hover,
.outline-btn:focus {
  border-color: #c80025;
  color: #c80025;
  background: #fff5f7;
}

.outline-btn.danger {
  border-color: #fecdd3;
  color: #dc2626;
}

.outline-btn.danger:hover,
.outline-btn.danger:focus {
  border-color: #dc2626;
  color: #b91c1c;
  background: #fef2f2;
}

.assistant-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.chat-panel {
  height: calc(100vh - 170px);
  min-height: 540px;
  display: grid;
  grid-template-rows: 1fr auto;
}

.messages {
  padding: 18px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message {
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.bubble {
  max-width: min(720px, 80%);
  white-space: pre-wrap;
  line-height: 1.7;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #1f2937;
}

.message.user .bubble {
  background: #e4002b;
  color: #fff;
}

.composer {
  border-top: 1px solid #e5e7eb;
  padding: 14px;
  display: grid;
  grid-template-columns: 1fr 96px;
  gap: 12px;
  align-items: end;
}

.side-panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: fit-content;
}

.side-panel .el-button {
  width: 100%;
  height: auto;
  min-height: 32px;
  margin-left: 0;
  padding: 8px 10px;
  white-space: normal;
  text-align: left;
  line-height: 1.35;
}

.panel-title {
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 4px;
}

.tip {
  margin-top: 10px;
}

@media (max-width: 900px) {
  .toolbar {
    display: grid;
    grid-template-columns: 1fr;
  }

  .actions {
    justify-content: flex-start;
  }

  .assistant-layout {
    grid-template-columns: 1fr;
  }

  .chat-panel {
    height: auto;
  }
}
</style>
