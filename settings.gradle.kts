rootProject.name = "BeastSummoner"
include(":BMLBuilder", ":CreatureCustomiser", ":PlaceNpc", ":TradeLibrary", ":WurmTestingHelper")
project(":BMLBuilder").projectDir = file("../BMLBuilder")
project(":CreatureCustomiser").projectDir = file("../CreatureCustomiser")
project(":PlaceNpc").projectDir = file("../PlaceNpc")
project(":WurmTestingHelper").projectDir = file("../WurmTestingHelper")
project(":TradeLibrary").projectDir = file("../TradeLibrary")

