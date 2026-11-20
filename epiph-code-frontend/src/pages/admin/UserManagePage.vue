<template>
  <div id="userManagePage">
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <p class="page-subtitle">检索平台账号，维护用户角色与基本资料。</p>
    </div>

    <!-- Filter Card -->
    <a-card class="filter-card">
      <a-form layout="inline" :model="searchParams" class="search-form" @finish="doSearch">
        <a-form-item label="用户账号">
          <a-input v-model:value="searchParams.userAccount" placeholder="输入账号查找" allow-clear />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="searchParams.userName" placeholder="输入用户名查找" allow-clear />
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
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userAvatar'">
            <a-avatar :src="record.userAvatar" :size="36" class="user-avatar">
              {{ record.userName?.charAt(0) || 'U' }}
            </a-avatar>
          </template>
          <template v-else-if="column.dataIndex === 'userRole'">
            <a-tag v-if="record.userRole === 'admin'" color="purple">管理员</a-tag>
            <a-tag v-else color="blue">普通用户</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" size="small" @click="openResetModal(record)">重置密码</a-button>
              <a-popconfirm
                title="确定要删除该用户账号吗？"
                ok-text="确认"
                cancel-text="取消"
                ok-type="danger"
                @confirm="doDelete(record.id)"
              >
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Reset Password Modal -->
    <a-modal
      v-model:open="resetModalOpen"
      title="重置用户密码"
      ok-text="确认重置"
      cancel-text="取消"
      :confirm-loading="resetLoading"
      @ok="handleResetPassword"
    >
      <p v-if="resetTargetUser" class="reset-target-tip">
        正在为账号 <strong>{{ resetTargetUser.userAccount }}</strong>
        <span v-if="resetTargetUser.userName">（{{ resetTargetUser.userName }}）</span> 重置登录密码：
      </p>
      <a-form layout="vertical">
        <a-form-item label="新密码（至少 8 位，留空则重置为 12345678）">
          <a-input-password
            v-model:value="resetPasswordValue"
            placeholder="输入新密码，默认 12345678"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { deleteUser, listUserVoByPage, resetUserPassword } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { SearchOutlined } from '@ant-design/icons-vue'

const columns = [
  {
    title: '用户 ID',
    dataIndex: 'id',
    width: 180,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 80,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '用户简介',
    dataIndex: 'userProfile',
    ellipsis: true,
  },
  {
    title: '权限角色',
    dataIndex: 'userRole',
    width: 120,
  },
  {
    title: '注册时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 170,
  },
]

const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'desc',
})

const data = ref<API.UserVO[]>([])
const loading = ref(false)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条用户`,
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPage(searchParams)
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
      pagination.current = searchParams.pageNum || 1
    } else {
      message.error('获取用户列表失败：' + res.data.message)
    }
  } catch {
    message.error('加载用户列表异常')
  } finally {
    loading.value = false
  }
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const resetSearch = () => {
  searchParams.userAccount = ''
  searchParams.userName = ''
  searchParams.pageNum = 1
  fetchData()
}

const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  pagination.pageSize = page.pageSize
  fetchData()
}

const resetModalOpen = ref(false)
const resetLoading = ref(false)
const resetTargetUser = ref<API.UserVO | null>(null)
const resetPasswordValue = ref('12345678')

const openResetModal = (record: API.UserVO) => {
  resetTargetUser.value = record
  resetPasswordValue.value = '12345678'
  resetModalOpen.value = true
}

const handleResetPassword = async () => {
  if (!resetTargetUser.value?.id) return
  const pwd = resetPasswordValue.value.trim()
  if (pwd && pwd.length < 8) {
    message.warning('新密码长度不能小于 8 位')
    return
  }
  resetLoading.value = true
  try {
    const res = await resetUserPassword({
      id: resetTargetUser.value.id,
      newPassword: pwd || '12345678',
    })
    if (res.data.code === 0 && res.data.data) {
      message.success(`已将 ${resetTargetUser.value.userAccount} 的密码重置为 ${pwd || '12345678'}`)
      resetModalOpen.value = false
    } else {
      message.error('重置密码失败：' + res.data.message)
    }
  } catch {
    message.error('重置密码请求异常')
  } finally {
    resetLoading.value = false
  }
}

const doDelete = async (id: number) => {
  if (!id) return
  try {
    const res = await deleteUser({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch {
    message.error('删除请求异常')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#userManagePage {
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

.user-avatar {
  background: #1A73E8;
  color: #FFFFFF;
  font-weight: 600;
}
</style>
