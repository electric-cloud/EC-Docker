public class CommonUtils {
    public static <E extends Enum<E>> boolean isValueInEnum(String value, Class<E> enumClass) {
        for (E e : enumClass.getEnumConstants()) {
            if(e.name().equals(value)) { return true; }
        }
        return false;
    }

    public static boolean printDiagMessage(String level, String message) {
        level = level.toUpperCase();
        if (!['ERROR','WARNING','INFO'].contains(level)) {
            return 0;
        }

        // \n[POSTP][%s]: %s :[%s][POSTP]\n
        def begin = "\n[POSTP][$level]: ";
        def end   = " :[$level][POSTP]\n";

        print(begin + message + end);

        return 1
    }

    public static boolean logInfoDiag(String message) {
        return printDiagMessage('INFO', message);
    }

    public static boolean logErrorDiag(String message) {
        return printDiagMessage('ERROR', message);
    }

    public static boolean logWarningDiag(String message) {
        return printDiagMessage('WARNING', message);
    }

}
