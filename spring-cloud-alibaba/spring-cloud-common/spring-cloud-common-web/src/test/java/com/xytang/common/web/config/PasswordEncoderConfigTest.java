package com.xytang.common.web.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Argon2id 密码编码器测试。
 *
 * <p>验证：
 * <ul>
 *   <li>编码输出以 {@code $argon2id$} 开头（OWASP 2024 推荐模式）</li>
 *   <li>编码串长度在 [90, 100] 字符区间（参数：salt=16B + key=32B + base64 padding）</li>
 *   <li>同一明文两次编码产生不同哈希（盐随机）</li>
 *   <li>matches 对正确明文返回 true，错误明文返回 false</li>
 *   <li>打印 admin123 的 Argon2id 哈希供 T028 SQL 脚本回填</li>
 * </ul>
 */
class PasswordEncoderConfigTest {

    private PasswordEncoderConfig config;
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() throws Exception {
        config = new PasswordEncoderConfig();
        setField("saltLength", 16);
        setField("keyLength", 32);
        setField("parallelism", 2);
        setField("memory", 16384);
        setField("iterations", 3);
        encoder = config.passwordEncoder();
    }

    private void setField(String name, int value) throws Exception {
        Field f = PasswordEncoderConfig.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(config, value);
    }

    @Test
    void encodeShouldReturnArgon2idHash() {
        String hash = encoder.encode("admin123");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$argon2id$"),
            "Argon2id 哈希应以 $argon2id$ 开头，实际：" + hash);
        int len = hash.length();
        assertTrue(len >= 90 && len <= 100,
            "Argon2id 编码串长度应在 [90, 100] 区间，实际：" + len + " 字符");
    }

    @Test
    void encodeShouldUseRandomSalt() {
        String h1 = encoder.encode("admin123");
        String h2 = encoder.encode("admin123");
        assertTrue(!h1.equals(h2), "两次编码应产生不同哈希（随机盐）");
        assertTrue(encoder.matches("admin123", h1));
        assertTrue(encoder.matches("admin123", h2));
    }

    @Test
    void matchesShouldRejectWrongPassword() {
        String hash = encoder.encode("admin123");
        assertTrue(encoder.matches("admin123", hash));
        assertTrue(!encoder.matches("wrongpass", hash));
    }

    @Test
    void printAdminPasswordHashForSeedSql() {
        String hash = encoder.encode("admin123");
        assertTrue(encoder.matches("admin123", hash));
        assertEquals(1, 1);
        System.out.println("[Argon2id for admin123 (paste into sca-system-init.sql)]");
        System.out.println(hash);
        System.out.println("[length=" + hash.length() + "]");
    }

    @Test
    void matchesShouldAcceptHashFromDatabaseSeed() {
        String dbHash = "$argon2id$v=19$m=16384,t=3,p=2$+27WTLFAqxSTRl5oyRAIjw$FDm+vGxZbK72A/m7fGobGmU6Kgg6RsyuHLHJnwyfXzc";
        assertTrue(encoder.matches("admin123", dbHash),
            "数据库 seed 中的 Argon2id 哈希必须能匹配 admin123");
    }
}
