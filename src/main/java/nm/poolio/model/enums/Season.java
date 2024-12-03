package nm.poolio.model.enums;

import lombok.Getter;

public enum Season {
  S_2024("2024");

  @Getter final String display;

  Season(String display) {
    this.display = display;
  }

  public static Season getCurrent() {
    return S_2024;
  }
}
