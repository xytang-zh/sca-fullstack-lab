package com.xytang.auth.service.impl;

import cloud.tianai.captcha.common.AnyMap;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import cloud.tianai.captcha.validator.impl.BasicCaptchaTrackValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * 归一化轨迹验证器：tianai 前端 SDK 记录的是页面绝对坐标，
 * 而 BasicCaptchaTrackValidator 的规则检测假设相对坐标（起点 ~0）。
 * 校验前将轨迹整体平移，使起点归零，再执行标准规则检测。
 */
public class TrackNormalizeCaptchaValidator extends BasicCaptchaTrackValidator {

    private static final int RELATIVE_ORIGIN_RANGE = 10;

    @Override
    public ApiResponse<?> valid(ImageCaptchaTrack imageCaptchaTrack, AnyMap imageCaptchaValidData) {
        ImageCaptchaTrack normalized = normalize(imageCaptchaTrack);
        return super.valid(normalized, imageCaptchaValidData);
    }

    private ImageCaptchaTrack normalize(ImageCaptchaTrack track) {
        List<ImageCaptchaTrack.Track> trackList = track.getTrackList();
        if (trackList == null || trackList.isEmpty()) {
            return track;
        }
        ImageCaptchaTrack.Track first = trackList.get(0);
        if (Math.abs(first.getX()) <= RELATIVE_ORIGIN_RANGE
                && Math.abs(first.getY()) <= RELATIVE_ORIGIN_RANGE) {
            return track;
        }
        List<ImageCaptchaTrack.Track> normalizedList = new ArrayList<>(trackList.size());
        for (ImageCaptchaTrack.Track t : trackList) {
            ImageCaptchaTrack.Track nt = new ImageCaptchaTrack.Track();
            nt.setX(t.getX() - first.getX());
            nt.setY(t.getY() - first.getY());
            nt.setT(t.getT());
            nt.setType(t.getType());
            normalizedList.add(nt);
        }
        ImageCaptchaTrack result = new ImageCaptchaTrack();
        result.setBgImageWidth(track.getBgImageWidth());
        result.setBgImageHeight(track.getBgImageHeight());
        result.setTemplateImageWidth(track.getTemplateImageWidth());
        result.setTemplateImageHeight(track.getTemplateImageHeight());
        result.setStartTime(track.getStartTime());
        result.setStopTime(track.getStopTime());
        result.setLeft(track.getLeft());
        result.setTop(track.getTop());
        result.setTrackList(normalizedList);
        result.setData(track.getData());
        return result;
    }
}
