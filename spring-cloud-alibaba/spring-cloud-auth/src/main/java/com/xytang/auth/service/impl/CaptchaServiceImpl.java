package com.xytang.auth.service.impl;

import com.xytang.auth.constant.AuthConstants;
import com.xytang.auth.service.CaptchaService;
import com.xytang.auth.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private static final char[] CHARS =
        "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CaptchaVO generate() {
        String key = UUID.randomUUID().toString();
        String code = randomCode(4);
        BufferedImage image = drawImage(code);
        String base64 = "data:image/png;base64," + toBase64(image);
        stringRedisTemplate.opsForValue().set(
            AuthConstants.CAPTCHA_PREFIX + key,
            code,
            Duration.ofMinutes(AuthConstants.CAPTCHA_TTL_MINUTES));
        return CaptchaVO.builder().captchaKey(key).captchaImg(base64).build();
    }

    @Override
    public boolean verify(String captchaKey, String input) {
        if (captchaKey == null || input == null || input.length() != 4) {
            return false;
        }
        String redisKey = AuthConstants.CAPTCHA_PREFIX + captchaKey;
        String stored = stringRedisTemplate.opsForValue().get(redisKey);
        // 校验后立即删除（一次性）
        stringRedisTemplate.delete(redisKey);
        return stored != null && stored.equalsIgnoreCase(input);
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < len; i++) {
            sb.append(CHARS[rnd.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    private BufferedImage drawImage(String code) {
        int w = 120, h = 40;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(220, 220, 220));
            for (int i = 0; i < 8; i++) {
                g.drawLine(
                    ThreadLocalRandom.current().nextInt(w),
                    ThreadLocalRandom.current().nextInt(h),
                    ThreadLocalRandom.current().nextInt(w),
                    ThreadLocalRandom.current().nextInt(h));
            }
            g.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 26));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(
                    ThreadLocalRandom.current().nextInt(50, 200),
                    ThreadLocalRandom.current().nextInt(50, 200),
                    ThreadLocalRandom.current().nextInt(50, 200)));
                g.drawString(String.valueOf(code.charAt(i)), 8 + i * 28, 28);
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    private String toBase64(BufferedImage img) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", bos);
            return java.util.Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("captcha image encode failed", e);
        }
    }
}
