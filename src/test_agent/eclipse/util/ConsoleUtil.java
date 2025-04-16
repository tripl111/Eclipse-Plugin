package test_agent.eclipse.util;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class ConsoleUtil {
    private static final String CONSOLE_NAME = "CoverAgent Console";
    private static MessageConsole console;
    private static MessageConsoleStream infoStream;
    private static MessageConsoleStream errorStream;

    public static MessageConsole getConsole() {
        if (console == null) {
            console = findConsole(CONSOLE_NAME);
            infoStream = console.newMessageStream();
            errorStream = console.newMessageStream();
            
            // Set colors for different message types
            Display.getDefault().asyncExec(() -> {
                errorStream.setColor(new Color(Display.getDefault(), 255, 0, 0)); // Red for errors
            });
        }
        return console;
    }

    private static MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        
        for (IConsole element : existing) {
            if (name.equals(element.getName())) {
                return (MessageConsole) element;
            }
        }
        
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[]{myConsole});
        return myConsole;
    }

    public static void writeInfo(String message) {
        getConsole();
        Display.getDefault().asyncExec(() -> {
            infoStream.println(message);
        });
    }

    public static void writeError(String message) {
        getConsole();
        Display.getDefault().asyncExec(() -> {
            errorStream.println("ERROR: " + message);
        });
    }

    public static void showConsole() {
        Display.getDefault().asyncExec(() -> {
            IConsoleManager conMan = ConsolePlugin.getDefault().getConsoleManager();
            conMan.showConsoleView(getConsole());
        });
    }
}