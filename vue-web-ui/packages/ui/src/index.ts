import type { GlobalThemeOverrides } from 'naive-ui'

export const UI_PACKAGE_NAME = '@sca/ui'

/** 品牌主色：indigo-500，与 portal/admin 渐变（indigo→violet）一致 */
export const primaryColor = '#6366f1'

/** 全局 Naive UI 主题覆盖：统一主色与圆角，两个应用共用 */
export const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor,
    primaryColorHover: '#818cf8',
    primaryColorPressed: '#4f46e5',
    primaryColorSuppl: '#818cf8',
    borderRadius: '8px',
    borderRadiusSmall: '6px'
  },
  Button: {
    borderRadiusMedium: '8px',
    borderRadiusLarge: '10px'
  },
  Card: {
    borderRadius: '12px'
  }
}
