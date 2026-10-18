package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.modelo.CierreCaja;
import com.parqueadero.modelo.MovimientoCaja;
import com.parqueadero.modelo.TipoMovimientoCaja;
import com.parqueadero.servicio.CajaService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PanelCaja extends JPanel {
    private final CajaService cajaService;

    private final JComboBox<TipoMovimientoCaja> comboTipo = new JComboBox<>(TipoMovimientoCaja.values());
    private final JTextField campoConcepto = new JTextField(20);
    private final JTextField campoMontoMovimiento = new JTextField(10);
    private final DefaultTableModel modeloMovimientos;
    private final JTable tablaMovimientos;

    private final JLabel labelIngresosParqueo = new JLabel();
    private final JLabel labelIngresosLavado = new JLabel();
    private final JLabel labelIngresosMensualidades = new JLabel();
    private final JLabel labelIngresosManuales = new JLabel();
    private final JLabel labelEgresosManuales = new JLabel();
    private final JTextField campoBaseInicial = new JTextField("0", 10);
    private final JTextField campoMontoContado = new JTextField(10);
    private final JLabel labelTotalEsperado = new JLabel();
    private final JLabel labelDiferencia = new JLabel();
    private final DefaultTableModel modeloCierres;

    public PanelCaja(CajaService cajaService) {
        this.cajaService = cajaService;

        setLayout(new GridLayout(1, 2, 16, 0));
        setOpaque(false);

        // --- Movimientos de caja (entradas/salidas manuales) ---
        campoConcepto.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Concepto (ej: compra de jabón)");
        campoMontoMovimiento.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Monto");
        JButton botonRegistrarMovimiento = Estilos.primario(new JButton("Registrar"));
        botonRegistrarMovimiento.addActionListener(e -> registrarMovimiento());

        JPanel formularioMovimiento = new JPanel(new GridBagLayout());
        formularioMovimiento.setOpaque(false);
        GridBagConstraints gm = new GridBagConstraints();
        gm.fill = GridBagConstraints.HORIZONTAL;
        gm.insets = new Insets(0, 0, 0, 8);
        gm.gridx = 0;
        formularioMovimiento.add(comboTipo, gm);
        gm.gridx = 1;
        gm.weightx = 1;
        formularioMovimiento.add(campoConcepto, gm);
        gm.gridx = 2;
        gm.weightx = 0.4;
        formularioMovimiento.add(campoMontoMovimiento, gm);
        gm.gridx = 3;
        gm.weightx = 0;
        gm.insets = new Insets(0, 0, 0, 0);
        formularioMovimiento.add(botonRegistrarMovimiento, gm);

        modeloMovimientos = new DefaultTableModel(new Object[]{"Hora", "Tipo", "Concepto", "Monto", "Usuario"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaMovimientos = Tablas.crear(modeloMovimientos);
        JPanel cuerpoMovimientos = new JPanel(new BorderLayout(0, 12));
        cuerpoMovimientos.setOpaque(false);
        cuerpoMovimientos.add(formularioMovimiento, BorderLayout.NORTH);
        cuerpoMovimientos.add(sinBorde(new JScrollPane(tablaMovimientos)), BorderLayout.CENTER);

        modeloCierres = new DefaultTableModel(new Object[]{"Fecha", "Cajero", "Base", "Esperado", "Contado", "Diferencia"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable tablaCierres = Tablas.crear(modeloCierres);

        JPanel columnaIzquierda = new JPanel(new GridLayout(2, 1, 0, 16));
        columnaIzquierda.setOpaque(false);
        columnaIzquierda.add(Tarjeta.conTitulo("Movimientos de caja de hoy",
                "Dinero que entra o sale aparte de los cobros", cuerpoMovimientos));
        columnaIzquierda.add(Tarjeta.conTitulo("Cierres anteriores", null, sinBorde(new JScrollPane(tablaCierres))));

        // --- Cierre de caja del día ---
        labelTotalEsperado.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        labelDiferencia.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        JButton botonCalcular = new JButton("Calcular");
        botonCalcular.addActionListener(e -> calcular());
        JButton botonGuardarCierre = Estilos.primario(new JButton("Guardar cierre del día"));
        botonGuardarCierre.addActionListener(e -> guardarCierre());

        Formulario datosCierre = new Formulario();
        datosCierre.agregar(filaValor("Ingresos por parqueo", labelIngresosParqueo));
        datosCierre.agregar(filaValor("Ingresos por lavado", labelIngresosLavado));
        datosCierre.agregar(filaValor("Ingresos por mensualidades", labelIngresosMensualidades));
        datosCierre.agregar(filaValor("Ingresos manuales", labelIngresosManuales));
        datosCierre.agregar(filaValor("Egresos manuales", labelEgresosManuales));
        datosCierre.campo("Base inicial de caja", campoBaseInicial);
        datosCierre.campo("Monto contado en caja", campoMontoContado);
        JPanel filaCalcular = new JPanel(new GridLayout(1, 2, 10, 0));
        filaCalcular.setOpaque(false);
        filaCalcular.add(botonCalcular);
        filaCalcular.add(botonGuardarCierre);
        datosCierre.agregar(filaCalcular);
        datosCierre.agregar(filaValor("Total esperado en caja", labelTotalEsperado));
        datosCierre.agregar(filaValor("Diferencia", labelDiferencia));

        JPanel contenedorCierre = new JPanel(new BorderLayout());
        contenedorCierre.setOpaque(false);
        contenedorCierre.add(datosCierre, BorderLayout.NORTH);
        add(columnaIzquierda);
        add(Tarjeta.conTitulo("Cierre de caja del día", "Cuadra lo que hay en caja contra lo que dice el sistema",
                contenedorCierre));

        refrescar();
    }

    private static JScrollPane sinBorde(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    /** Fila "concepto ........ valor" con el valor alineado a la derecha. */
    private static JPanel filaValor(String texto, JLabel valor) {
        JPanel fila = new JPanel(new BorderLayout());
        fila.setOpaque(false);
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(Tema.textoSecundario());
        valor.setHorizontalAlignment(SwingConstants.RIGHT);
        fila.add(etiqueta, BorderLayout.WEST);
        fila.add(valor, BorderLayout.EAST);
        return fila;
    }

    public void refrescar() {
        List<MovimientoCaja> movimientos = cajaService.obtenerMovimientosDeHoy();
        modeloMovimientos.setRowCount(0);
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");
        for (MovimientoCaja m : movimientos) {
            modeloMovimientos.addRow(new Object[]{
                    m.getFecha().format(fmtHora), m.getTipo(), m.getConcepto(), "$" + m.getMonto(),
                    m.getUsuario() != null ? m.getUsuario() : ""
            });
        }

        CajaService.ResumenCaja resumen = cajaService.calcularResumen(0);
        labelIngresosParqueo.setText("$" + resumen.ingresosParqueo());
        labelIngresosLavado.setText("$" + resumen.ingresosLavado());
        labelIngresosMensualidades.setText("$" + resumen.ingresosMensualidades());
        labelIngresosManuales.setText("$" + resumen.ingresosManuales());
        labelEgresosManuales.setText("$" + resumen.egresosManuales());
        labelTotalEsperado.setText("-");
        labelDiferencia.setText("-");
        labelDiferencia.setForeground(UIManager.getColor("Label.foreground"));

        modeloCierres.setRowCount(0);
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (CierreCaja c : cajaService.obtenerHistorialCierres()) {
            modeloCierres.addRow(new Object[]{
                    c.getFechaHoraCierre().format(fmtFecha), c.getUsuario() != null ? c.getUsuario() : "",
                    "$" + c.getBaseInicial(), "$" + c.getTotalEsperado(), "$" + c.getMontoContado(),
                    formatearDiferencia(c.getDiferencia())
            });
        }
    }

    private void registrarMovimiento() {
        try {
            TipoMovimientoCaja tipo = (TipoMovimientoCaja) comboTipo.getSelectedItem();
            int monto = Integer.parseInt(campoMontoMovimiento.getText().trim());
            String concepto = campoConcepto.getText();
            if (tipo == TipoMovimientoCaja.INGRESO) {
                cajaService.registrarIngreso(concepto, monto);
            } else {
                cajaService.registrarEgreso(concepto, monto);
            }
            campoConcepto.setText("");
            campoMontoMovimiento.setText("");
            refrescar();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El monto debe ser un número", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calcular() {
        try {
            int base = Integer.parseInt(campoBaseInicial.getText().trim());
            int contado = Integer.parseInt(campoMontoContado.getText().trim());
            CajaService.ResumenCaja resumen = cajaService.calcularResumen(base);
            labelTotalEsperado.setText("$" + resumen.totalEsperado());
            int diferencia = contado - resumen.totalEsperado();
            labelDiferencia.setText(formatearDiferencia(diferencia));
            labelDiferencia.setForeground(colorDiferencia(diferencia));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La base y el monto contado deben ser números", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarCierre() {
        try {
            int base = Integer.parseInt(campoBaseInicial.getText().trim());
            int contado = Integer.parseInt(campoMontoContado.getText().trim());
            int confirmar = JOptionPane.showConfirmDialog(this,
                    "¿Guardar el cierre de caja de hoy? Esta acción queda registrada en el historial.",
                    "Confirmar cierre", JOptionPane.YES_NO_OPTION);
            if (confirmar != JOptionPane.YES_OPTION) {
                return;
            }
            cajaService.guardarCierre(base, contado);
            JOptionPane.showMessageDialog(this, "Cierre de caja guardado correctamente");
            refrescar();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La base y el monto contado deben ser números", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatearDiferencia(int diferencia) {
        if (diferencia == 0) {
            return "Cuadra ($0)";
        }
        if (diferencia < 0) {
            return "Faltante: $" + Math.abs(diferencia);
        }
        return "Sobrante: $" + diferencia;
    }

    private Color colorDiferencia(int diferencia) {
        if (diferencia == 0) {
            return Tema.verde();
        }
        return diferencia < 0 ? Tema.rojo() : Tema.ambar();
    }
}
