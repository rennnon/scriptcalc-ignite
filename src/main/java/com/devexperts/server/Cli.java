package com.devexperts.server;

import com.devexperts.common.Utils;
import org.apache.ignite.Ignite;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.function.Consumer;

public class Cli {
    private final Terminal terminal;
    private final CommandLine commandLine;

    private Cli(Ignite ignite, CacheLocalKeysTracker tracker) {
        terminal = new Terminal();
        terminal.setInputListener(this::executeCommand);

        String nodeId = ignite.cluster().localNode().consistentId().toString();
        Window window = new Window(nodeId, "Server node has been started.\nType \"help\" for commands list.",
                new CliAction("info", () -> new PrintNodeInfo(terminal, ignite).run()),
                new CliAction("keys", () -> new PrintLocalKeys(terminal, tracker).run()));
        terminal.setWindow(window);

        commandLine = new CommandLine(new RootCommand(), new CommandsFactory(terminal, ignite, tracker));
        commandLine.setOut(new PrintWriter(terminal));
        commandLine.setErr(new PrintWriter(terminal));
    }

    public static void start(Ignite ignite, CacheLocalKeysTracker tracker) {
        new Cli(ignite, tracker);
    }

    private void executeCommand(String command) {
        String[] args = Arrays.stream(command.split(" "))
                .filter(arg -> !arg.isEmpty())
                .toArray(String[]::new);
        if (args.length > 0)
            commandLine.execute(args);
    }

    private static class CliAction extends AbstractAction {
        private final Runnable action;

        private CliAction(String name, Runnable action) {
            super(name);
            this.action = action;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.run();
        }
    }

    /**
     * For commands which need constructor params like the terminal;
     */
    public static class CommandsFactory implements CommandLine.IFactory {
        private final CommandLine.IFactory fallback = CommandLine.defaultFactory();
        private final Terminal terminal;
        private final Ignite ignite;
        private final CacheLocalKeysTracker tracker;

        public CommandsFactory(Terminal terminal, Ignite ignite, CacheLocalKeysTracker tracker) {
            this.terminal = terminal;
            this.ignite = ignite;
            this.tracker = tracker;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <K> K create(Class<K> cls) throws Exception {
            if (cls == PrintNodeInfo.class) {
                return (K) new PrintNodeInfo(terminal, ignite);
            } else if (cls == PrintLocalKeys.class) {
                return (K) new PrintLocalKeys(terminal, tracker);
            }
            return fallback.create(cls);
        }
    }

    @CommandLine.Command(
            name = "",
            description = "Simple commands for ignite cluster",
            subcommands = {PrintNodeInfo.class, PrintLocalKeys.class, CommandLine.HelpCommand.class})
    private static class RootCommand implements Runnable {
        @Override
        public void run() {
            //does nothing
        }
    }

    @CommandLine.Command(name = "info",
            description = "Info about the current ignite node")
    private static class PrintNodeInfo implements Runnable {
        private final Terminal terminal;
        private final Ignite ignite;

        private PrintNodeInfo(Terminal terminal, Ignite ignite) {
            this.terminal = terminal;
            this.ignite = ignite;
        }

        @Override
        public void run() {
            Utils.printNodeStats(ignite, new PrintStream(terminal));
        }
    }

    @CommandLine.Command(name = "keys",
            description = "List of all cache keys which are local for the current node")
    private static class PrintLocalKeys implements Runnable {
        private final Terminal terminal;
        private final CacheLocalKeysTracker tracker;

        private PrintLocalKeys(Terminal terminal, CacheLocalKeysTracker tracker) {
            this.terminal = terminal;
            this.tracker = tracker;
        }

        @Override
        public void run() {
            terminal.append("Local keys for \"" + tracker.getTrackedCacheInfo().name() + "\" cache: ");
            terminal.append(tracker.getLocalKeysSnapshot().toString());
            terminal.append(System.lineSeparator());
        }
    }


    private static class Window {
        private static final int WIDTH = 800;
        private static final int HEIGHT = 600;

        private final JTextArea output;
        private final JTextField input;
        private Consumer<String> inputListener;

        private Window(String name, String greetings, Action... actions) {
            JFrame frame = new JFrame(name);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(WIDTH, HEIGHT);
            frame.setResizable(true);
            Container contentPane = frame.getContentPane();
            contentPane.setLayout(new BorderLayout());

            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            for (Action action : actions)
                buttonsPanel.add(new JButton(action));
            contentPane.add(buttonsPanel, BorderLayout.NORTH);

            output = new JTextArea(greetings + System.lineSeparator());
            output.setLineWrap(true);
            output.setEditable(false);
            JScrollPane textAreaScroll = new JScrollPane(output,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            frame.add(textAreaScroll);
            contentPane.add(textAreaScroll, BorderLayout.CENTER);


            input = new JTextField();
            input.addKeyListener(
                    new KeyAdapter() {
                        @Override
                        public void keyTyped(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyChar() == '\n') {
                                onInputFinished();
                            }
                        }
                    }
            );
            contentPane.add(input, BorderLayout.SOUTH);

            frame.setVisible(true);
        }

        public void setInputListener(Consumer<String> listener) {
            inputListener = listener;
        }

        public void append(String text) {
            output.append(text);
            output.setCaretPosition(output.getDocument().getLength());
        }

        private void onInputFinished() {
            String command = input.getText();
            input.setText("");
            if (inputListener != null)
                inputListener.accept(command);
        }
    }

    private static class Terminal extends OutputStream {
        private Consumer<String> commandConsumer;
        private Window window;

        public void setWindow(Window window) {
            this.window = window;
            window.setInputListener(commandConsumer);
        }

        void setInputListener(Consumer<String> listener) {
            commandConsumer = listener;
            if (window != null)
                window.setInputListener(listener);
        }


        void append(String text) {
            if (window == null) {
                System.out.println(text);
            } else {
                window.append(text);
            }
        }

        @Override
        public void write(int b) {
            append(String.valueOf((char) b));
        }
    }
}
