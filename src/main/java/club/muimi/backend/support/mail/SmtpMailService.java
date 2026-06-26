package club.muimi.backend.support.mail;

import club.muimi.backend.common.api.ErrorCode;
import club.muimi.backend.common.enums.EmailCodeScene;
import club.muimi.backend.config.SmtpProperties;
import club.muimi.backend.exception.BusinessException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpMailService implements MailService {

    private final JavaMailSender javaMailSender;
    private final SmtpProperties smtpProperties;

    public SmtpMailService(JavaMailSender javaMailSender, SmtpProperties smtpProperties) {
        this.javaMailSender = javaMailSender;
        this.smtpProperties = smtpProperties;
    }

    @Override
    public void sendVerificationCode(String email, String code, EmailCodeScene scene) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(smtpProperties.getUsername());
        message.setTo(email);
        message.setSubject(buildSubject(scene));
        message.setText(buildBody(code, scene));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "验证码邮件发送失败，请稍后再试");
        }
    }

    private String buildSubject(EmailCodeScene scene) {
        return switch (scene) {
            case REGISTER -> "FRESH 招新平台注册验证码";
            case RESET_PASSWORD -> "FRESH 招新平台密码重置验证码";
        };
    }

    private String buildBody(String code, EmailCodeScene scene) {
        String sceneText = switch (scene) {
            case REGISTER -> "注册";
            case RESET_PASSWORD -> "重置密码";
        };
        return """
                您好：

                您正在进行 FRESH 招新平台%s操作。
                本次验证码为：%s

                验证码 5 分钟内有效，请勿泄露给他人。
                如果这不是您的操作，请忽略本邮件。
                """.formatted(sceneText, code);
    }
}
