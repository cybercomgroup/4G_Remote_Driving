# 4G_Remote_Driving

Albin Hellqvist         - albhel@student.chalmers.se

Betim Raqi              - betim@student.chalmers.se

Tasdikul Huda           - tasdikul@student.chalmers.se

Victor Christoffersson  - cvictor@student.chalmers.se

Android-applikation
På applikationens startsida skall ip-adress och port anges till den VPN-server som används som mellanhand till applikation och RPi. VPN-tjänst på telefonen kan också behöva ställas in. Utan en VPN-tjänst så fungerar bilen endast i lokala nätverk. 

När en uppkoppling har etablerats så kommer du till huvudsidan av applikationen, där du kan styra bilen och se data från videoströmmen. Klicka på “Call”-knappen för att aktivera videoströmmen (detta brukar ta ca. 10 sekunder). Var noga med att klicka på “Hang up”-knappen innan applikationen stängs ner.

Bilen
När strömsladd kopplas in till Raspberryn och dess motorhat, så startas VPN, stream, server automatiskt. Dock kommer ni få göra en egen VPN eftersom vi som skapade den inte kommer ha uppe vår VPN-server. Det finns mycket filer för detta på skrivbordet och massa länkar i webbläsaren.

Servern körs vanligtvis på port 3000 och streamen på port 8080.
