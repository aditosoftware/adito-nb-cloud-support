package de.adito.nbm.ssp.checkout;

import lombok.NonNull;
import org.jetbrains.annotations.*;

import java.awt.*;

/**
 * Gibt die Standardwerte f?r URLs, Projektpfad und Datenbankpfad an
 * F?r die URLs wird eine eigene Klasse verwendet, die URL und Zugangsdaten verbindet
 *
 * @author s.danner, 19.09.13
 */
public interface IDefaultSettingsCallback
{

  /**
   * Gibt einen Default-Pfad zur?ck, wo die Projekt-Dateien gespeichert werden sollen
   */
  @Nullable
  String getDefaultProjectPath();

  /**
   * Gibt die Scrollgeschwindigkeit f?r die Detailseite zur?ck
   * Die Angabe erfolgt als positive Zahl ("Unit"-Increment)
   */
  int getDefaultScrollSpeed();

  /**
   * Gibt die Default-Hintergrundfarbe f?r ein VisualPanel des Wizards zur?ck
   */
  @NonNull
  Color getDefaultBackgroundColor();
}
