package nm.poolio.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum NflTeam {
  SF("San Francisco 49ers"),
  MIN("Minnesota Vikings"),
  HOU("Houston Texans"),
  PHI("Philadelphia Eagles"),
  LAR("Los Angeles Rams"),
  WAS("Washington Commanders"),
  ARI("Arizona Cardinals"),
  NYG("New York Giants"),
  ATL("Atlanta Falcons"),
  KC("Kansas City Chiefs"),
  BUF("Buffalo Bills"),
  NO("New Orleans Saints"),
  TB("Tampa Bay Buccaneers"),
  JAC("Jacksonville Jaguars"),
  BAL("Baltimore Ravens"),
  DAL("Dallas Cowboys"),
  CIN("Cincinnati Bengals"),
  GB("Green Bay Packers"),
  NYJ("New York Jets"),
  SEA("Seattle Seahawks"),
  PIT("Pittsburgh Steelers"),
  DEN("Denver Broncos"),
  LV("Las Vegas Raiders"),
  LAC("Los Angeles Chargers"),
  NE("New England Patriots"),
  IND("Indianapolis Colts"),
  MIA("Miami Dolphins"),
  CLE("Cleveland Browns"),
  CHI("Chicago Bears"),
  CAR("Carolina Panthers"),
  TEN("Tennessee Titans"),
  DET("Detroit Lions"),
  TIE("TIE"),
  TBD("Place holder");

//  CLEMSON("Clemson"),
//  GEORGIA("Georgia"),
//  PENN("Penn State"),
//  WV("West Viginia"),
//  MIAMI("Miami FL"),
//  FL("Florida"),
//  ND("Notre Dame"),
//  TX_AM("Texas A and M"),
//  NM("New Mexico"),
//  AZ_ST("Arizona State"),
//  USC("Univ of So. Cal"),
//  LSU("Louisiana State Univ"),
//  NEB("Nebraska Corn huskers"),
//  UTEP("Texas El Paso Univ"),
//  BOS("Boston College"),
//  FL_ST("Florida State"),
//  JMU("James Madison Dukes"),
//  CHAR("Charlotte 49'er");

  @Getter final String fullName;
}
