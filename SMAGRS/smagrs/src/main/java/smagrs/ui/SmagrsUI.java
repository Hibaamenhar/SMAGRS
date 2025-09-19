package smagrs.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.function.Consumer;

public class SmagrsUI extends JFrame {

    /* --------- champs formulaire --------- */
    private final JTextField requester = new JTextField("hiba@fpo.ac.ma");
    private final JTextField dateField = new JTextField(LocalDate.now().toString()); // YYYY-MM-DD
    private final JTextField timeField = new JTextField("10:00");                    // HH:mm
    private final JSpinner   hoursSp   = new JSpinner(new SpinnerNumberModel(2, 1, 12, 1));
    private final JSpinner   capacitySp= new JSpinner(new SpinnerNumberModel(30, 1, 999, 1));
    private final JTextField eqField   = new JTextField("projecteur,tableau");
    private final JTextField purpose   = new JTextField("Cours SMA");

    /* --------- tableau des réservations --------- */
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Salle", "Début", "Fin", "Capacité", "Demandeur"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    /* --------- barre d’état + callback --------- */
    private final JLabel status = new JLabel("Prêt");
    private Consumer<String> onSubmitJson;

    public SmagrsUI() {
        super("SMAGRS – Réservations de Salles (JADE + Swing)");
        /* Look & Feel moderne */
        try { UIManager.setLookAndFeel(new FlatLightLaf()); } catch (Exception ignore) {}

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);

        /* conteneur principal */
        var root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        /* ---- bandeau titre ---- */
        var title = new JLabel("SMAGRS – Réservations de Salles (JADE + Swing)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        root.add(title, BorderLayout.NORTH);

        /* ---- zone centrale: formulaire + bouton + tableau ---- */
        var center = new JPanel(new BorderLayout(12, 12));
        root.add(center, BorderLayout.CENTER);

        // panneau formulaire à gauche
        var formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Demande"));
        var gc = new GridBagConstraints();
        gc.insets = new Insets(6, 10, 6, 10);
        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill   = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0;

        int row = 0;
        addRow(formPanel, gc, row++, "Demandeur", requester);
        addRow(formPanel, gc, row++, "Date (YYYY-MM-DD)", dateField);
        addRow(formPanel, gc, row++, "Heure (HH:mm)", timeField);
        addRow(formPanel, gc, row++, "Durée (heures)", hoursSp);
        addRow(formPanel, gc, row++, "Capacité min.", capacitySp);
        addRow(formPanel, gc, row++, "Équipements (CSV)", eqField);
        addRow(formPanel, gc, row++, "Objet", purpose);

        center.add(formPanel, BorderLayout.CENTER);

        // bouton d’envoi à droite
        var sendPanel = new JPanel(new GridBagLayout());
        sendPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        var sendBtn = new JButton("Envoyer la demande");
        sendBtn.setFont(sendBtn.getFont().deriveFont(Font.BOLD, 16f));
        sendBtn.setPreferredSize(new Dimension(260, 60));
        sendBtn.addActionListener(e -> send());
        sendPanel.add(sendBtn);
        center.add(sendPanel, BorderLayout.EAST);

        // tableau en bas
        var tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Réservations confirmées"));
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(tablePanel, BorderLayout.SOUTH);

        /* ---- barre d’état ---- */
        var statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EmptyBorder(6, 12, 6, 12));
        status.setEnabled(true);
        statusPanel.add(status, BorderLayout.WEST);
        root.add(statusPanel, BorderLayout.PAGE_END);
    }

    private static void addRow(JPanel panel, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        var l = new JLabel(label);
        l.setPreferredSize(new Dimension(160, 28)); // largeur fixe labels
        panel.add(l, gc);

        gc.gridx = 1; gc.weightx = 1;
        field.setPreferredSize(new Dimension(320, 28));
        panel.add(field, gc);
    }

    /* ---------- interactions ---------- */

    private void send() {
        try {
            var dt = LocalDateTime.of(
                    LocalDate.parse(dateField.getText().trim()),
                    LocalTime.parse(timeField.getText().trim())
            );
            long h   = ((Number) hoursSp.getValue()).longValue();
            int cap  = ((Number) capacitySp.getValue()).intValue();
            Set<String> eq = Set.of(eqField.getText().toLowerCase().replace(" ", "").split(","));

            String json = """
                {"requester":"%s","slot":{"start":"%s","hours":%d},"capacity":%d,"equipment":"%s","purpose":"%s"}
                """.formatted(
                    requester.getText().trim(), dt, h, cap, String.join(",", eq), purpose.getText().trim()
                ).replace("\n","");

            if (onSubmitJson != null) onSubmitJson.accept(json);
            setStatus("Demande envoyée.");
        } catch (Exception ex) {
            setStatus("Erreur de saisie : " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Vérifiez la date (YYYY-MM-DD) et l’heure (HH:mm)\n\n" + ex.getMessage(),
                    "Saisie invalide", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void onSubmit(Consumer<String> handler) { this.onSubmitJson = handler; }

    public void addBookingRow(String room, String start, String end, int capacity, String requester) {
        SwingUtilities.invokeLater(() -> model.addRow(new Object[]{ room, start, end, capacity, requester }));
    }

    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> status.setText(text));
    }
}
