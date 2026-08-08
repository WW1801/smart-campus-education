/**
 * 共享当前学期状态，避免应用外壳和数据看板分别硬编码学年学期。
 * 接口不可用时保留明确的“未取得”状态，不回退到示例学期。
 */
import { computed, readonly, ref } from 'vue'
import request from '../utils/request'

const currentSemester = ref(null)
const semesterLoading = ref(false)
const semesterError = ref(false)
let pendingRequest = null

const semesterLabel = computed(() => currentSemester.value?.name || '当前学期未取得')

const loadCurrentSemester = async ({ force = false } = {}) => {
  if (currentSemester.value && !force) return currentSemester.value
  if (pendingRequest && !force) return pendingRequest

  semesterLoading.value = true
  semesterError.value = false
  pendingRequest = request.get('/semester/list', { skipErrorMessage: true })
    .then((res) => {
      const semesters = Array.isArray(res.data) ? res.data : []
      currentSemester.value = semesters.find(item => ['current', 'active'].includes(item.status)) || null
      semesterError.value = !currentSemester.value
      return currentSemester.value
    })
    .catch(() => {
      currentSemester.value = null
      semesterError.value = true
      return null
    })
    .finally(() => {
      semesterLoading.value = false
      pendingRequest = null
    })

  return pendingRequest
}

export const useCurrentSemester = () => ({
  currentSemester: readonly(currentSemester),
  semesterLabel,
  semesterLoading: readonly(semesterLoading),
  semesterError: readonly(semesterError),
  loadCurrentSemester
})

