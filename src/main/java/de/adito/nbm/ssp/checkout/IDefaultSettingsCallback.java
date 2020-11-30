package de.adito.nbm.ssp.checkout;

import org.jetbrains.annotations.*;

import java.awt.*;

/**
 * Gibt die Standardwerte für URLs, Projektpfad und Datenbankpfad an
 * Für die URLs wird eine eigene Klasse verwendet, die URL und Zugangsdaten verbindet
 *
 * @author s.danner, 19.09.13
 */
public interface IDefaultSettingsCallback
{

  /**
   * Gibt einen Default-Pfad zurück, wo die Projekt-Dateien gespeichert werden sollen
   */
  @Nullable
  String getDefaultProjectPath();

  /**
   * Gibt die Scrollgeschwindigkeit für die Detailseite zurück
   * Die Angabe erfolgt als positive Zahl ("Unit"-Increment)
   */
  int getDefaultScrollSpeed();

  /**
   * Gibt die Default-Hintergrundfarbe für ein VisualPanel des Wizards zurück
   */
  @NotNull
  Color getDefaultBackgroundColor();
}
