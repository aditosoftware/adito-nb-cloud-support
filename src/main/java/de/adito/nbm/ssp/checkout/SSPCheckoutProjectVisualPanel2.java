package de.adito.nbm.ssp.checkout;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.git.*;
import javax.swing.*;
import java.awt.*;

/**
 * The visualPanel of the wizard in which the location of the project to create are entered
 *
 * @author s.danner, 13.09.13
 */
public class SSPCheckoutProjectVisualPanel2 extends JPanel
{
  private JButton projectLocationBrowseButton;
  private JLabel projectLocationLabel;
  private JTextField projectNameTextField;
  private JTextField projectLocationTextField;
  private JLabel projectNameLabel;
  private JComboBox<IRef> gitBranchComboBox;
  private JLabel gitBranchNameLabel;

  public SSPCheckoutProjectVisualPanel2()
  {
    initComponents();
  }

  /**
   * Enables or disables all components
   *
   * @param pEnabled <tt>true</tt> if all components should be enabled
   */
  public void enableComps(boolean pEnabled)
  {
    projectLocationBrowseButton.setEnabled(pEnabled);
    projectNameTextField.setEnabled(pEnabled);
    projectLocationTextField.setEnabled(pEnabled);
    projectLocationLabel.setEnabled(pEnabled);
    projectNameLabel.setEnabled(pEnabled);
  }

  /**
   * intiates the visual components
   */
  private void initComponents()
  {
    projectLocationTextField = new JTextField();
    projectNameTextField = new JTextField();

    projectNameLabel = new JLabel(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectVisualPanel2.class,
                                                                              "SSPCheckoutProjectVisualPanel2.projectNameLabel.text"));
    projectLocationLabel = new JLabel(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectVisualPanel2.class,
                                                                                  "SSPCheckoutProjectVisualPanel2.projectLocationLabel.text"));

    projectLocationBrowseButton = new JButton(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectVisualPanel2.class,
                                                                                          "SSPCheckoutProjectVisualPanel2.projectLocationBrowseButton.text"));
    projectLocationTextField.setText(SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectVisualPanel2.class,
                                                                                 "SSPCheckoutProjectVisualPanel2.projectLocationTextField.text"));
    gitBranchNameLabel = new JLabel("Branch/Tag: ");
    gitBranchComboBox = new JComboBox<>();
    gitBranchComboBox.setRenderer(createListRenderer());

    GroupLayout layout = new GroupLayout(this);
    this.setLayout(layout);
    this.setPreferredSize(new Dimension(700, 300));
    layout.setHorizontalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                          .addContainerGap()
                          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(projectNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(projectLocationLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(gitBranchNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                          .addGap(10, 10, 10)
                          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(projectNameTextField, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                                        .addComponent(projectLocationTextField, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                                        .addComponent(gitBranchComboBox, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
                          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(projectLocationBrowseButton))
                          .addContainerGap())
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                          .addContainerGap()
                          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(projectNameLabel)
                                        .addComponent(projectNameTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                          .addGap(18, 18, 18)
                          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(projectLocationTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(projectLocationLabel)
                                        .addComponent(projectLocationBrowseButton))
                          .addGap(18, 18, 18)
                          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(gitBranchNameLabel)
                                        .addComponent(gitBranchComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                          .addGap(18, 18, 18)
                          .addContainerGap(199, Short.MAX_VALUE))
    );
  }

  /**
   * Returns the name of the step
   *
   * @return name as string
   */
  @Override
  public String getName()
  {
    return SSPCheckoutProjectWizardIterator.getMessage(SSPCheckoutProjectVisualPanel2.class, "SSPCheckoutProjectVisualPanel2.stepName.text");
  }

  public JButton getProjectLocationBrowseButton()
  {
    return projectLocationBrowseButton;
  }

  public JTextField getProjectLocationTextField()
  {
    return projectLocationTextField;
  }

  public JTextField getProjectNameTextField()
  {
    return projectNameTextField;
  }

  public JComboBox<IRef> getGitBranchComboBox()
  {
    return gitBranchComboBox;
  }

  public JLabel getGitBranchNameLabel()
  {
    return gitBranchNameLabel;
  }

  private static DefaultListCellRenderer createListRenderer() {
    return new DefaultListCellRenderer() {

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus)
      {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(value != null){
          if(value instanceof ITag)
          {
            ITag tag = (ITag) value;
            this.setText("Tag: " + tag.getName());
          }
          if (value instanceof IRemoteBranch)
          {
            IRemoteBranch branch = (IRemoteBranch) value;
            this.setText("Branch: " + branch.getName());
          }
        }
        return this;
      }
    };
  }

}
