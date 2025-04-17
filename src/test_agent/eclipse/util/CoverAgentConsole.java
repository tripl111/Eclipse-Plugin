package test_agent.eclipse.util;


import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.*;

import org.eclipse.ui.console.*;

public final class CoverAgentConsole {

    private static final String CONSOLE_NAME = "CoverAgent";
    private static MessageConsole console;

    /** Lazily create / find the console and show it. */
    private static MessageConsole getConsole() {
        if (console != null) return console;

        IConsoleManager mgr = ConsolePlugin.getDefault().getConsoleManager();
        for (IConsole c : mgr.getConsoles()) {
            if (CONSOLE_NAME.equals(c.getName())) {
                console = (MessageConsole) c;
                return console;
            }
        }
        console = new MessageConsole(CONSOLE_NAME, null);
        mgr.addConsoles(new IConsole[] { console });
        mgr.showConsoleView(console);          // brings it to the front
        return console;
    }

    /** Bridge `java.util.logging` to the console. */
    private static class ConsoleHandler extends Handler {
        private final MessageConsoleStream stream = getConsole().newMessageStream();
        private final Formatter fmt = new SimpleFormatter();

        @Override public void publish(LogRecord r) { stream.print(fmt.format(r)); }
        @Override public void flush()               { try {
			stream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} }
        @Override public void close()               { try {
			stream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} }
    }

    /** Call once (e.g. in Activator.start or the first job you run). */
    public static void install() {
        // 1. Pipe JUL → console
        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers()) root.removeHandler(h); // optional
        root.addHandler(new ConsoleHandler());

        // 2. Pipe System.out / System.err → console as well
        MessageConsoleStream out = getConsole().newMessageStream();
        MessageConsoleStream err = getConsole().newMessageStream();
        err.setActivateOnWrite(true); // bring console to front on errors

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(err, true));
    }

    private CoverAgentConsole() {}  // static‑utility
}
