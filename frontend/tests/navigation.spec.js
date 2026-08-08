import { describe, expect, it } from 'vitest'
import { getVisibleNavigation } from '../config/navigation'

const pathsFor = roleId => getVisibleNavigation(roleId).flatMap(section => section.items.map(item => item.path))

describe('角色导航', () => {
  it('管理员拥有完整的 21 项编号菜单', () => {
    const sections = getVisibleNavigation('1')
    const items = sections.flatMap(section => section.items)
    expect(items).toHaveLength(21)
    expect(items[0]).toMatchObject({ path: '/home/dashboard', number: '01' })
    expect(items[1]).toMatchObject({ path: '/home/profile', number: '02' })
    expect(items.at(-1)).toMatchObject({ path: '/home/agent/warning', number: '21' })
  })

  it.each(['1', '2', '3'])('角色 %s 可见 AI 分析入口', roleId => {
    expect(pathsFor(roleId)).toContain('/home/agent/analysis')
  })

  it.each(['4', '5'])('角色 %s 不显示 AI 分析入口', roleId => {
    expect(pathsFor(roleId)).not.toContain('/home/agent/analysis')
  })
})
