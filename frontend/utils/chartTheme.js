/** 从全局设计变量读取图表颜色，避免 ECharts 形成独立视觉体系。 */
export const getChartTheme = () => {
  const styles = getComputedStyle(document.documentElement)
  const token = (name, fallback) => styles.getPropertyValue(name).trim() || fallback

  return {
    ink: token('--ink', '#142b4a'),
    text: token('--text-secondary', '#4a5568'),
    muted: token('--text-muted', '#7d8fa0'),
    line: token('--line', '#d4deea'),
    surface: token('--surface', '#ffffff'),
    blue: token('--blue', '#295da8'),
    blueBright: token('--blue-bright', '#4a8fd6'),
    sage: token('--sage', '#54785e'),
    vermilion: token('--vermilion', '#b7352a')
  }
}
