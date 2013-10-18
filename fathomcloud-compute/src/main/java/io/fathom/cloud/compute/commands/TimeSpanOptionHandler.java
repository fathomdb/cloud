package io.fathom.cloud.compute.commands;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import com.fathomdb.TimeSpan;

public class TimeSpanOptionHandler extends OptionHandler<TimeSpan> {

    public TimeSpanOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super TimeSpan> setter) {
        super(parser, option, setter);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        String s = params.getParameter(0);
        TimeSpan value;
        try {
            value = TimeSpan.parse(s);
        } catch (Exception e) {
            if (option.isArgument()) {
                throw new CmdLineException(owner, Messages.ILLEGAL_OPERAND.format(option.toString(), s));
            } else {
                throw new CmdLineException(owner, Messages.ILLEGAL_OPERAND.format(params.getParameter(-1), s));
            }
        }

        setter.addValue(value);
        return 1;
    }

    @Override
    public String getDefaultMetaVariable() {
        return "TIMESPAN";
    }

}
