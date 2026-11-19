<template>
  <div id="chatManagePage">
    <div class="page-header">
      <h1 class="page-title">对话管理</h1>
      <p class="page-subtitle">检索平台历史消息与生成过程，支持跨应用排查与快速跳转。</p>
    </div>

    <!-- Filter Card -->
    <a-card class="filter-card">
      <a-form layout="inline" :model="searchParams" class="search-form" @finish="doSearch">
        <a-form-item label="消息内容">
          <a-input v-model:value="searchParams.message" placeholder="输入消息关键字" allow-clear />
        </a-form-item>
        <a-form-item label="消息类型">
          <a-select
            v-model:value="searchParams.messageType"
            placeholder="全部类型"
            style="min-width: 130px"
            allow-clear
          >
            <a-select-option value="">全部类型</a-select-option>
            <a-select-option value="user">用户消息</a-select-option>
            <a-select-option value="assistant">AI 消息</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="应用 ID">
          <a-input v-model:value="searchParams.appId" placeholder="应用 ID" allow-clear />
        </a-form-item>
        <a-form-item label="用户 ID">
          <a-input v-model:value="searchParams.userId" placeholder="用户 ID" allow-clear />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="resetSearch">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- Data Table Card -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="data"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
        :scroll="{ x: 1100 }"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'message'">
            <a-tooltip :title="record.message">
              <div class="message-text-cell">{{ record.message }}</div>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'messageType'">
            <a-tag :color="record.messageType === 'user' ? 'blue' : 'purple'">
              {{ record.messageType === 'user' ? '用户输入' : 'AI 响应' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewAppChat(record.appId)">
                进入工作台
              </a-button>
              <a-popconfirm
                title="确定要删除这条对话记录吗？"
                ok-text="确认"
                cancel-text="取消"
                ok-type="danger"
                @confirm="deleteMessage(record.id)"
              >
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'
import { formatTime } from '@/utils/time'
import { SearchOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
    fixed: 'left',
  },
  {
    title: '消息内容',
    dataIndex: 'message',
    width: 320,
  },
  {
    title: '类型',
    dataIndex: 'messageType',
    width: 100,
  },
  {
    title: '应用 ID',
    dataIndex: 'appId',
    width: 90,
  },
  {
    title: '用户 ID',
    dataIndex: 'userId',
    width: 90,
  },
  {
    title: '记录时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 160,
    fixed: 'right',
  },
]

const data = ref<API.ChatHistory[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  message: '',
  messageType: '',
  appId: undefined,
  userId: undefined,
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAllChatHistoryByPageForAdmin({ ...searchParams })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取对话记录失败：' + res.data.message)
    }
  } catch {
    message.error('加载对话记录异常')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})

const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条记录`,
  }
})

const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const resetSearch = () => {
  searchParams.message = ''
  searchParams.messageType = ''
  searchParams.appId = undefined
  searchParams.userId = undefined
  searchParams.pageNum = 1
  fetchData()
}

const viewAppChat = (appId: number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}`)
  }
}

const deleteMessage = async (id: number | undefined) => {
  if (!id) return
  try {
    message.success('删除成功')
    fetchData()
  } catch {
    message.error('删除失败')
  }
}
</script>

<style scoped>
#chatManagePage {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  margin-bottom: 4px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #0F172A;
  letter-spacing: -0.02em;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: #64748B;
}

.filter-card,
.table-card {
  border-radius: 12px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.message-text-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #334155;
}
</style>
