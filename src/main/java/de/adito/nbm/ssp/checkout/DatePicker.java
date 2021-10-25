package de.adito.nbm.ssp.checkout;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;

public class DatePicker extends JPanel
{
  private static final int MIN_YEAR = 1950;
  private static final int MAX_YEAR = 2100;
  private final JComboBox<Integer> dayCombobox;
  private final JComboBox<Integer> monthCombobox;
  private final JComboBox<Integer> yearCombobox;

  DatePicker(){
    super();
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    dayCombobox = new JComboBox<>();
    for(int i = 1; i<= 31; i++){
      dayCombobox.addItem(i);
    }

    monthCombobox = new JComboBox<>();
    for(int i = 1; i<= 12; i++){
      monthCombobox.addItem(i);
    }

    yearCombobox = new JComboBox<>();
    for(int i = MIN_YEAR; i <= MAX_YEAR; i++){
      yearCombobox.addItem(i);
    }
    yearCombobox.setSelectedItem(Calendar.getInstance().get(Calendar.YEAR));

    dayCombobox.setPreferredSize(new Dimension(5, 0));
    monthCombobox.setPreferredSize(new Dimension(5, 0));
    yearCombobox.setPreferredSize(new Dimension(10, 0));

    add(new JLabel("Day:"));
    add(Box.createRigidArea(new Dimension(5, 0)));
    add(dayCombobox);
    add(Box.createRigidArea(new Dimension(5, 0)));
    add(new JLabel("Month:"));
    add(Box.createRigidArea(new Dimension(5, 0)));
    add(monthCombobox);
    add(Box.createRigidArea(new Dimension(5, 0)));
    add(new JLabel("Year:"));
    add(Box.createRigidArea(new Dimension(5, 0)));
    add(yearCombobox);
    setVisible(false);
  }

  public JComboBox<Integer> getDayCombobox(){
    return dayCombobox;
  }

  public JComboBox<Integer> getMonthCombobox(){
    return monthCombobox;
  }

  public JComboBox<Integer> getYearCombobox(){
    return yearCombobox;
  }

  public int getCurrentDay()
  {
    if(dayCombobox.getSelectedItem() != null)
      return (Integer) dayCombobox.getSelectedItem();
    else
      return -1;
  }

  public int getCurrentMonth()
  {
    if(monthCombobox.getSelectedItem() != null)
      return (Integer) monthCombobox.getSelectedItem();
    else
      return -1;
  }

  public int getCurrentYear()
  {
    if(yearCombobox.getSelectedItem() != null)
      return (Integer) yearCombobox.getSelectedItem();
    else
      return -1;
  }
}
