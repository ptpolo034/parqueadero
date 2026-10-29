package com.parqueadero.ui;

import com.parqueadero.servicio.ReporteService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PanelReportes extends JPanel {
    private static final int MAX_DIAS_GRAFICO = 31;

    private final ReporteService reporteService;

    private final JSpinner spinnerDesde;
    private final JSpinner spinnerHasta;
    private final TarjetaKpi kpiParqueo = new TarjetaKpi("Parqueo", "carro", Estilos.Tono.AMBAR, false);
    private final TarjetaKpi kpiLavado = new TarjetaKpi("Lavado", "lavado", Estilos.Tono.ACENTO, false);
    private final TarjetaKpi kpiMensualidades = new TarjetaKpi("Mensualidades", "mensualidades", Estilos.Tono.VERDE, false);
    private final TarjetaKpi kpiTotal = new TarjetaKpi("Total del período", "dinero", Estilos.Tono.ACENTO, false);
    private final GraficoBarras grafico = new GraficoBarras();
    private final Tarjeta tarjetaGrafico;

    public PanelReportes(ReporteService reporteService) {
        this.reporteService = reporteService;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        spinnerDesde = crearSpinnerFecha();
        spinnerHasta = crearSpinnerFecha();
        JButton botonConsultar = Estilos.primario(new JButton("Consultar"));
        botonConsultar.addActionListener(e -> refrescar());
        JButton botonExportar = new JButton("Exportar a CSV...");
        botonExportar.addActionListener(e -> exportarCSV());

        JPanel panelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelFiltro.setOpaque(false);
        panelFiltro.add(new JLabel("Desde"));
        panelFiltro.add(Box.createHorizontalStrut(8));
        panelFiltro.add(spinnerDesde);
        panelFiltro.add(Box.createHorizontalStrut(16));
        panelFiltro.add(new JLabel("Hasta"));
        panelFiltro.add(Box.createHorizontalStrut(8));
        panelFiltro.add(spinnerHasta);
        panelFiltro.add(Box.createHorizontalStrut(16));
        panelFiltro.add(botonConsultar);
        panelFiltro.add(Box.createHorizontalStrut(10));
        panelFiltro.add(botonExportar);

        JPanel indicadores = new JPanel(new GridLayout(1, 4, 16, 0));
        indicadores.setOpaque(false);
        indicadores.add(kpiParqueo);
        indicadores.add(kpiLavado);
        indicadores.add(kpiMensualidades);
        indicadores.add(kpiTotal);

        JPanel arriba = new JPanel(new BorderLayout(0, 16));
        arriba.setOpaque(false);
        arriba.add(Tarjeta.conTitulo("Período", "Elige el rango de fechas del reporte", panelFiltro), BorderLayout.NORTH);
        arriba.add(indicadores, BorderLayout.CENTER);
        add(arriba, BorderLayout.NORTH);

        tarjetaGrafico = Tarjeta.conTitulo("Ingresos por día", "Se muestra para rangos de hasta " + MAX_DIAS_GRAFICO + " días",
                grafico);
        add(tarjetaGrafico, BorderLayout.CENTER);
    }

    private JSpinner crearSpinnerFecha() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        spinner.setValue(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        return spinner;
    }

    private LocalDate leerFecha(JSpinner spinner) {
        Date fecha = (Date) spinner.getValue();
        return fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public void refrescar() {
        LocalDate desde = leerFecha(spinnerDesde);
        LocalDate hasta = leerFecha(spinnerHasta);
        if (hasta.isBefore(desde)) {
            JOptionPane.showMessageDialog(this, "La fecha 'hasta' no puede ser anterior a 'desde'",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        kpiParqueo.actualizar(dinero(reporteService.ingresosParqueoEnRango(desde, hasta)), "Cobros por hora");
        kpiLavado.actualizar(dinero(reporteService.ingresosLavadoEnRango(desde, hasta)), "Servicios de lavado");
        kpiMensualidades.actualizar(dinero(reporteService.ingresosMensualidadesEnRango(desde, hasta)), "Mensualidades cobradas");
        kpiTotal.actualizar(dinero(reporteService.ingresosTotalesEnRango(desde, hasta)), "Suma de todo lo anterior");

        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        tarjetaGrafico.setVisible(dias <= MAX_DIAS_GRAFICO);
        if (dias <= MAX_DIAS_GRAFICO) {
            List<String> etiquetas = new ArrayList<>();
            List<Integer> valores = new ArrayList<>();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dias > 10 ? "d" : "EEE d", new java.util.Locale("es", "CO"));
            for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
                etiquetas.add(d.format(fmt).replace(".", ""));
                valores.add(reporteService.ingresosTotalesEnRango(d, d));
            }
            grafico.establecerDatos(etiquetas, valores);
        }
    }

    private static String dinero(int valor) {
        return Formato.dinero(valor);
    }

    private void exportarCSV() {
        LocalDate desde = leerFecha(spinnerDesde);
        LocalDate hasta = leerFecha(spinnerHasta);
        if (hasta.isBefore(desde)) {
            JOptionPane.showMessageDialog(this, "La fecha 'hasta' no puede ser anterior a 'desde'",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new java.io.File("reporte_" + desde + "_a_" + hasta + ".csv"));
        int resultado = selector.showSaveDialog(this);
        if (resultado != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            reporteService.exportarCSV(selector.getSelectedFile().toPath(), desde, hasta);
            JOptionPane.showMessageDialog(this, "Reporte exportado correctamente");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
