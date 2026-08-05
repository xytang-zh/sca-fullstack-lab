package com.xytang.auth.enums;

/**
 * 设备类型（用于 Sa-Token 多端会话治理与踢人下线）
 */
public enum DeviceTypeEnum {

    /** 桌面端（浏览器访问管理后台） */
    PC,
    /** 移动端 App */
    APP,
    /** 移动端 Web（H5 浏览器） */
    WEB,
    /** 小程序（微信/支付宝等） */
    MINI
}
