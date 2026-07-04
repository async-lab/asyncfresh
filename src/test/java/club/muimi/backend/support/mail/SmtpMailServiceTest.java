package club.muimi.backend.support.mail;

import club.muimi.backend.common.api.ErrorCode;
import club.muimi.backend.common.enums.EmailCodeScene;
import club.muimi.backend.config.BrandProperties;
import club.muimi.backend.config.SmtpProperties;
import club.muimi.backend.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpMailServiceTest {

    @Test
    void shouldSendVerificationMailUsingConfiguredAccount() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpProperties smtpProperties = buildMailProperties();
        BrandProperties brandProperties = buildBrandProperties();
        smtpProperties.validate();
        SmtpMailService mailService = new SmtpMailService(mailSender, smtpProperties, brandProperties);

        mailService.sendVerificationCode("user@example.com", "123456", EmailCodeScene.REGISTER);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldFailFastWhenMailConfigurationMissing() {
        SmtpProperties smtpProperties = new SmtpProperties();

        assertThatThrownBy(smtpProperties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_SMTP_HOST");
    }

    @Test
    void shouldWrapMailExceptionAsBusinessException() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpProperties smtpProperties = buildMailProperties();
        smtpProperties.validate();
        BrandProperties brandProperties = buildBrandProperties();
        SmtpMailService mailService = new SmtpMailService(mailSender, smtpProperties, brandProperties);
        doThrow(new MailSendException("smtp error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> mailService.sendVerificationCode("user@example.com", "123456", EmailCodeScene.RESET_PASSWORD))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
                .hasMessage("验证码邮件发送失败，请稍后再试");
    }

    private SmtpProperties buildMailProperties() {
        SmtpProperties smtpProperties = new SmtpProperties();
        smtpProperties.setHost("smtp.example.com");
        smtpProperties.setPort(465);
        smtpProperties.setUsername("noreply@example.com");
        smtpProperties.setPassword("auth-code");
        return smtpProperties;
    }

    private BrandProperties buildBrandProperties() {
        return new BrandProperties();
    }
}
