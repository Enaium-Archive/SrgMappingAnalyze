package cn.enaium.sma.exception;

/**
 * Project: SrgMappingAnalyze
 * Author: Enaium
 */
public class NoArgumentException extends NullPointerException {
    public NoArgumentException(String argument) {
        super("No " + argument + ", try -A" + argument + "=value");
    }
}
