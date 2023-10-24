package net.bytle.smtp.milter;

import net.bytle.smtp.SmtpMessage;

import java.util.function.UnaryOperator;

public interface SmtpMilter extends UnaryOperator<SmtpMessage> {

}
