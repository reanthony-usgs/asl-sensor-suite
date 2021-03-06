package asl.sensor.gui;

import asl.sensor.ExperimentFactory;
import asl.sensor.experiment.NoiseNineExperiment;
import asl.sensor.input.DataStore;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import javax.swing.JComboBox;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Panel for 9-input self noise. Similar to 3-input self noise (NoisePanel)
 * but includes multiple plots, one for each linear axis in 3D space
 * (north-south, east-west, up-down) and a combo box to select them
 *
 * @author akearns - KBRWyle
 */
public class NoiseNinePanel extends NoisePanel {

  private static final long serialVersionUID = -8049021432657749975L;
  private final JComboBox<String> plotSelection;
  private JFreeChart northChart, eastChart, verticalChart;

  /**
   * Construct panel and lay out its components
   *
   * @param experiment Enum to get relevant experiment backend from factory (NoiseNineExperiment)
   */
  public NoiseNinePanel(ExperimentFactory experiment) {
    super(experiment);

    expResult = experiment.createExperiment();

    set = false;

    for (int i = 0; i < 3; ++i) {
      int num = i + 1;
      channelType[3 * i] = "North sensor " + num + " (RESP required)";
      channelType[(3 * i) + 1] = "East sensor " + num + " (RESP required)";
      channelType[(3 * i) + 2] = "Vertical sensor " + num + " (RESP required)";
    }

    this.setLayout(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();

    northChart =
        ChartFactory.createXYLineChart(expType.getName() + " (North)",
            "", "", null);
    eastChart =
        ChartFactory.createXYLineChart(expType.getName() + " (East)",
            "", "", null);
    verticalChart =
        ChartFactory.createXYLineChart(expType.getName() + " (Vertical)",
            "", "", null);
    for (JFreeChart chart : getCharts()) {
      chart.getXYPlot().setDomainAxis(getXAxis());
      chart.getXYPlot().setRangeAxis(getYAxis());
    }

    chart = northChart;
    chartPanel.setChart(chart);

    removeAll(); // get rid of blank spacer JPanel from super
    // (everything else will get redrawn)

    constraints.fill = GridBagConstraints.BOTH;
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weightx = 1.0;
    constraints.weighty = 1.0;
    constraints.gridwidth = 3;
    constraints.anchor = GridBagConstraints.CENTER;
    add(chartPanel, constraints);

    // place the other UI elements in a single row below the chart
    constraints.gridwidth = 1;
    constraints.weighty = 0.0;
    constraints.weightx = 0.0;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.fill = GridBagConstraints.NONE;
    constraints.gridy += 1;
    constraints.gridx = 0;
    add(freqSpaceBox, constraints);

    constraints.gridx += 1;
    constraints.weightx = 1.0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.CENTER;
    add(save, constraints);

    // combo box to select items
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx += 1;
    constraints.weightx = 0;
    constraints.anchor = GridBagConstraints.WEST;
    plotSelection = new JComboBox<>();
    plotSelection.addItem("North component plot");
    plotSelection.addItem("East component plot");
    plotSelection.addItem("Vertical component plot");
    plotSelection.addActionListener(this);
    add(plotSelection, constraints);

    revalidate();

  }

  /**
   * Get string of data to used when building PDFs in specific circumstances
   *
   * @param experiment Experiment to extract data from
   * @return String representing experiment data (rotation angles)
   */
  public static String getInsetString(NoiseNineExperiment experiment) {
    double[] angles = experiment.getNorthAngles();
    StringBuilder sb = new StringBuilder();
    sb.append("Angle of rotation of north sensor 2 (deg): ");
    sb.append(DECIMAL_FORMAT.get().format(Math.toDegrees(angles[0])));
    sb.append("\nAngle of rotation of north sensor 3 (deg): ");
    sb.append(DECIMAL_FORMAT.get().format(Math.toDegrees(angles[1])));
    sb.append("\n");
    angles = experiment.getEastAngles();
    sb.append("Angle of rotation of east sensor 2 (deg): ");
    sb.append(DECIMAL_FORMAT.get().format(Math.toDegrees(angles[0])));
    sb.append("\nAngle of rotation of east sensor 3 (deg): ");
    sb.append(DECIMAL_FORMAT.get().format(Math.toDegrees(angles[1])));
    return sb.toString();
  }

  @Override
  public void actionPerformed(ActionEvent event) {

    super.actionPerformed(event);

    if (event.getSource() == plotSelection) {

      int index = plotSelection.getSelectedIndex();
      switch (index) {
        case 0:
          chart = northChart;
          break;
        case 1:
          chart = eastChart;
          break;
        default:
          chart = verticalChart;
          break;
      }

      chartPanel.setChart(chart);
      chartPanel.setMouseZoomable(true);
    }

  }

  @Override
  protected void drawCharts() {

    int index = plotSelection.getSelectedIndex();

    switch (index) {
      case 0:
        chart = northChart;
        break;
      case 1:
        chart = eastChart;
        break;
      default:
        chart = verticalChart;
        break;
    }

    chartPanel.setChart(chart);
    chartPanel.setMouseZoomable(true);

  }

  @Override
  public JFreeChart[] getCharts() {
    return new JFreeChart[]{northChart, eastChart, verticalChart};
  }



  @Override
  public int panelsNeeded() {
    return 9;
  }

  @Override
  public void updateData(final DataStore dataStore) {

    set = true;

    final boolean freqSpaceImmutable = freqSpaceBox.isSelected();

    NoiseNineExperiment noiseExperiment = (NoiseNineExperiment) expResult;
    noiseExperiment.setFreqSpace(freqSpaceImmutable);

    expResult.runExperimentOnData(dataStore);

    for (int j = 0; j < 3; ++j) {
      XYSeriesCollection timeseries = expResult.getData().get(j);

      for (int i = 0; i < NOISE_PLOT_COUNT; ++i) {
        String name = (String) timeseries.getSeriesKey(i);
        System.out.println(name);
        Color plotColor = COLORS[i % 3];
        seriesColorMap.put(name, plotColor);
        System.out.println(name + "," + plotColor);
        if (i >= 3) {
          System.out.println(name + "," + i);
          seriesDashedSet.add(name);
        }

      }
    }

    set = true;

    NoiseNineExperiment nineExperiment = (NoiseNineExperiment) expResult;
    String[] insetStrings = nineExperiment.getInsetStrings();
    XYPlot plot;

    northChart = buildChart(expResult.getData().get(0));
    northChart.setTitle("Self-noise (NORTH)");
    plot = northChart.getXYPlot();
    setTitle(plot, insetStrings[0]);

    eastChart = buildChart(expResult.getData().get(1));
    eastChart.setTitle("Self-noise (EAST)");
    plot = eastChart.getXYPlot();
    setTitle(plot, insetStrings[1]);

    verticalChart = buildChart(expResult.getData().get(2));
    verticalChart.setTitle("Self-noise (VERTICAL)");
    plot = verticalChart.getXYPlot();
    setTitle(plot, insetStrings[2]);

  }

}
