<template>
  <div id="appManagePage">
    <div class="page-header">
      <h1 class="page-title">应用管理</h1>
      <p class="page-subtitle">浏览与维护全平台应用工程、配置精选状态及管理上线记录。</p>
    </div>

    <!-- Filter Card -->
    <a-card class="filter-card">
      <a-form layout="inline" :model="searchParams" class="search-form" @finish="doSearch">
        <a-form-item label="应用名称">
          <a-input v-model:value="searchParams.appName" placeholder="输入名称查找" allow-clear />
        </a-form-item>
        <a-form-item label="创建者 ID">
          <a-input v-model:value="searchParams.userId" placeholder="输入用户 ID" allow-clear />
        </a-form-item>
        <a-form-item label="生成类型">
          <a-select
            v-model:value="searchParams.codeGenType"
            placeholder="全部生成类型"
            style="min-width: 140px"
            allow-clear
          >
            <a-select-option value="">全部类型</a-select-option>
            <a-select-option
              v-for="option in CODE_GEN_TYPE_OPTIONS"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-select-option>
          </a-select>
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
          <template v-if="column.dataIndex === 'cover'">
            <div class="cover-thumb">
              <a-image v-if="record.cover" :src="record.cover" :width="64" :height="44" class="thumb-img" />
              <span v-else class="no-cover-text">无封面</span>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'initPrompt'">
            <a-tooltip :title="record.initPrompt">
              <div class="prompt-text">{{ record.initPrompt }}</div>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'codeGenType'">
            <a-tag color="indigo">{{ formatCodeGenType(record.codeGenType) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'priority'">
            <a-tag v-if="record.priority === 99" color="gold">★ 精选</a-tag>
            <span v-else class="text-muted">{{ record.priority || 0 }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'deployedTime'">
            <span v-if="record.deployedTime" class="text-success">
              {{ formatTime(record.deployedTime) }}
            </span>
            <span v-else class="text-muted">未部署</span>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.dataIndex === 'user'">
            <UserInfo :user="record.user" size="small" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="editApp(record)">编辑</a-button>
              <a-button
                type="link"
                size="small"
                :class="{ 'featured-link': record.priority === 99 }"
                @click="toggleFeatured(record)"
              >
                {{ record.priority === 99 ? '取消精选' : '设为精选' }}
              </a-button>
              <a-popconfirm
                title="确定要彻底删除该应用及其工程版本吗？"
                ok-text="确认删除"
                cancel-text="取消"
                ok-type="danger"
                @confirm="deleteApp(record.id)"
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
import { listAppVoByPageByAdmin, deleteAppByAdmin, updateAppByAdmin } from '@/api/appController'
import { CODE_GEN_TYPE_OPTIONS, formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'
import UserInfo from '@/components/UserInfo.vue'
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
    title: '应用名称',
    dataIndex: 'appName',
    width: 160,
  },
  {
    title: '封面',
    dataIndex: 'cover',
    width: 90,
  },
  {
    title: '初始提示词',
    dataIndex: 'initPrompt',
    width: 220,
  },
  {
    title: '生成架构',
    dataIndex: 'codeGenType',
    width: 120,
  },
  {
    title: '精选权重',
    dataIndex: 'priority',
    width: 90,
  },
  {
    title: '部署状态',
    dataIndex: 'deployedTime',
    width: 170,
  },
  {
    title: '创建者',
    dataIndex: 'user',
    width: 140,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 170,
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
    fixed: 'right',
  },
]

const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  appName: '',
  userId: undefined,
  codeGenType: '',
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppVoByPageByAdmin({ ...searchParams })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取应用列表失败：' + res.data.message)
    }
  } catch {
    message.error('加载应用列表异常')
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
    showTotal: (total: number) => `共 ${total} 个应用`,
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
  searchParams.appName = ''
  searchParams.userId = undefined
  searchParams.codeGenType = ''
  searchParams.pageNum = 1
  fetchData()
}

const editApp = (app: API.AppVO) => {
  router.push(`/app/edit/${app.id}`)
}

const toggleFeatured = async (app: API.AppVO) => {
  if (!app.id) return
  const newPriority = app.priority === 99 ? 0 : 99
  try {
    const res = await updateAppByAdmin({
      id: app.id,
      priority: newPriority,
    })
    if (res.data.code === 0) {
      message.success(newPriority === 99 ? '已将该应用设为精选' : '已取消精选')
      fetchData()
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch {
    message.error('操作异常')
  }
}

const deleteApp = async (id: number | undefined) => {
  if (!id) return
  try {
    const res = await deleteAppByAdmin({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch {
    message.error('删除异常')
  }
}
</script>

<style scoped>
#appManagePage {
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

.cover-thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 44px;
  border-radius: 6px;
  overflow: hidden;
  background: #F1F5F9;
  border: 1px solid #E2E8F0;
}

.no-cover-text {
  font-size: 11px;
  color: #94A3B8;
}

.prompt-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #334155;
}

.text-success {
  color: #10B981;
}

.text-muted {
  color: #94A3B8;
}

.featured-link {
  color: #F59E0B;
}
</style>
