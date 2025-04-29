package com.wang.wangpicture.manager.email;

import com.wang.wangpicture.config.EmailConfig;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * 发送邮件管理类
 */
@Component
public class EmailSendManager {
    // 这个会自动读取 application.properties 中的配置
    @Resource
    private JavaMailSender emailSender;
    @Resource
    private EmailConfig emailConfig;

    /**
     * todo: 发送注册邮件
     * @param toEmail 接收者邮箱
     * @param code 验证码
     */

    public void sendRegisterEmail(String toEmail, String code) {
        // 创建邮件消息对象
        MimeMessage mimeMessage = emailSender.createMimeMessage();

        try {
            // 使用 MimeMessageHelper 来构建邮件内容
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setFrom(emailConfig.getUsername());  // 设置发件人
            messageHelper.setTo(toEmail);  // 设置收件人
            messageHelper.setSubject("智能云图库 --- 注册验证码");  // 设置邮件主题
            messageHelper.setText("您的注册验证码是: " + code);  // 设置邮件正文
            // 发送邮件
            emailSender.send(mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("发送邮件失败", e);
        }
    }

    /**
     * 发送邀请邮件（团队空间邀请）
     * @param link 邀请链接
     * @param toEmail 邀请人邮箱
     */
    public void sendInviteEmail(String link, String toEmail) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String htmlContent = "<h3>点击以下链接进行操作：</h3>"
                    + "<p><a href='" + link + "' target='_blank'>点击这里</a></p>"
                    + "<p>如果无法点击，请复制以下链接到浏览器：</p>"
                    + "<p>" + link + "</p>";
            //虽然这个等同host，但是这个不会自动注入
            helper.setFrom(emailConfig.getUsername());// 读取配置类中的发件人邮箱
            helper.setTo(toEmail);  // 接收者邮箱
            helper.setSubject("智能云图库 --- 加入团队空间邀请");
            helper.setText(htmlContent, true); // true 表示是 HTML 格式
            emailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
