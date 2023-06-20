# 🗳 Election Record JSON serialization (proposed specification)

draft 6/20/2023

1. This is the evolving version 2 of the Election Record. It is not backwards compatible with version 1.
2. All fields must be present unless marked as optional.
3. A missing (optional) String should be internally encoded as null (not empty string), to agree with python hashing.
4. proto_version = 1.9.0 [MAJOR.MINOR.PATCH](https://semver.org/)

Differences with JSON 1.53 

* serialization follows the 
* hashing follows the spec in 1.5.1
* coefficients.json (lagrange coefficients) are no longer needed
* context.json divided into two parts:
  1. electionConfig.json has the configuration parameters set before the KeyCeremony
  2. electionInitialized.json has output of the KeyCeremony, including the guardians (so separate guardian files are not needed)
* some renaming of fields to match the 1.9 spec vocabulary

### constants.json

````
{
   "name": "production group, low memory use, 4096 bits",
   "large_prime": "FFFFFFFF...FFFFFFFFF",
   "small_prime": "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF43",
   "cofactor":  "100000000...B6B9D8AE0F",
   "generator": "36036FED2...B6B9D8AE0F"
}
````
* addition of a "name" field, implementation dependent

### manifest.json

* unchanged

### electionConfig.json

````
{
    "config_version": "v2.0",
    "number_of_guardians": 5,
    "quorum": 3,
    "election_date": "date",
    "jurisdiction_info": "juris",
    "parameter_base_hash": "AB91D83C3DC3FEB76E57C2783CFE2CA85ADB4BC01FC5123EEAE3124CC3FB6CDE",
    "manifest_hash": "837F1489FB799C6B3065A8A8411A5F672AD50700ED2B416C7E9789FA6143C818",
    "election_base_hash": "2F43AF7A46973482884752A6D1B027087AD795027FC025094E4BAABBABE60F22"
}
````

* as specified in section 3.1

````
{
    "joint_public_key": "A344E61C4578173BDC615F1EB277B3F8EF0499F72491EF6C7ABA56839A08270E754752A8B75FA385C258B539EA1B637CE6C48D85532236E259245A257C3ACD6222CF0630DBD533210D7BD1ABD54040BE4BEC4625B3038F6FADE9DBD9A126D0FA1F8D8DBB00E6EE631F6151AC079230FEE6867E181E5BEC1A4FD76E97DC072A172723DCC6272A2B9515EF1E530AB362447DAFE30C8E70A1074CD29143497015B22D9660218FCD076ACFC29B03F460E62B26357F556D407CC43A1FC3E2EF159995BC5D869CF46E9B64D64DE7F73512C4509DA3E2B1CF0F04ECC810C3FB5AE18A2267A19709061355E1DF8EAAC0CF1B316F7E1A24681C1ACCBE26EA630F3AD04D07ECCB4BD95928F95DF26F197EAE03A0675F5F578C8A0C41D50F3D51A83DAA0DE2C7C8FAF2D0885C934B6E276692B457D443A8EB502C10D9FA436D968F3AF467C7BF7FAA547DC450D4FEE3AE0E1E36C54584B816056867617D0EE998AABCEE2A95DEF07109D98E48407AF5CEB196FBCCB4EE06FF0A6E06091565C176E7F12A35625287DE903A8C90D63614BF72ACCCA91E763FF85F79881F559C09BC6CCF321FDE9F44DC5BB25C1D4D148B7F40869CB324C24E63BFE8F8280FA323682F2F500B708F6E79EE5897ED2E49E62E7F9C572141010F6FE9421537CC0B4B58D37CF03E8F14E368F87154A56349DE535974A6A48282206F84EEAAFFEDD4F148307C8F9065",
    "extended_base_hash": "25CC99F007D158D7E661E660CACBC190497623FFDD15B7FFFB0E3939A2E5CA29",
    "guardians": [
        {
            "guardian_id": "guardian1",
            "x_coordinate": 1,
            "coefficient_proofs": [
                {
                    "public_key": "E56EE02FD7CA9A9628FE9FC41659465DCFD8628632179F7543CADC6F9C3D71260F30D5FF561FD831C09263AF181B953A91E17B77883F98E9E24CB869AA5028D127C4DBACDFE2E2111F368F7D576F5CB0DECF18DBBCE9FF4208EA59CD5FD4EEB61264969087D04C98A3EFB297AC357B1058A83553EE7FE20D1A53065EDF65D372DD50A08EDC10FC2BB6AF20047E827CE87CB47D71B6856CC9AFE0332E7B81DB0DEA93AFBD6F093643BFB440CDFA1F8192EE06A47DD67377BC7404C1D2D4931BFFC6EE48EFDC4C4800338B4A34A458B56B7E2BE14E86BCFCC8B0CDFF9D98ECDE5023210379C3C74C6C8DEC52ABC2077251C6E8C918E60CFA302E2A80F5F004D1D63DADD75061CA991B532C26521C653D5E585E87CEE891E291F220C50369D02B157E9BBE0908C21473E9722D9429C4FC8D6DB8B3C04F2CC6D19F9CA619AF59BE855C08D588068111DAFF62646D4483E8087EDCF6775AA52C69500040FF78C299762F3B2548F59E562AE58CEDA2A1BC09778203956EF5118BCF3DC4F57C4EAD70C704B29F7628C1A708C76AC919DB87712B0902D0F0C5F921190296C861FCB3B702E1B086A2729DE0BBA42D1B4BC72B7574A0595851C7AE2C4C81B6ECE88815213CD51167AB244E950A3D0408DAD1F570AFB8CA65C940A1138E1F7C5A5710C943D2FA0D7B99CC46C29603F3D1A829958C7DB5C4085B2469EF469A1015EAEE47E927",
                    "challenge": "45DBF0A739D987C7AD07A56CD067BBBA92199917813F26E8CB353A20D4E1AC93",
                    "response": "7B55A0BE4C0E65F14E4AE9D2EA1B262926450D6ABE4B8A5F335647AEDCB72615"
                },
                {
                    "public_key": "7A9CF83EA4FF09A1AE3778192B2A59A5A8243F81D4E4C014FCE0139CACC86FDB5769C6E1CA04D33076E1F546FA9AAD65FD357782F8C6FF948B82F474ADAABD62F68BE4A2058A7BD6953E2E244FCC00BDEDEE5B865E369A8C077EBD95A6A46443FECE55EE9B3E177B4C1F13C2B6946419A5D9754E409182FD60AA175569926D663E6EB9D8DC1EF0E4EA5191E9A1AAF521BC4A0DCAF28E26FED3A2AE70DCDFE592AA2C93ADCA3E1995779A255866BCD4BD760C3CAC054BFE675FAFAEFAD2F481B6A3BE568965111B8D8DB59B1840FB54345BD83F4ADC075F0514E0E0232567D40A3A98FB8AC2A0BD412C28683E6DB38E35633CDE0E088E00D58FC8F24142FF82D09EF71B186B2826F17A1C4532DAC02E92F5BA456398DC62F7FA79AC88B56BA86D126C38147936F0E060B45632966208B4F0CE3D7E34481D2BC3DC7006DE1A3FC916ED24D55CE9CB7834E9AC7C62E43C2242FD283D6C0D3FAB300DA72298DDF24EEAF0BBCA73ECB66B2E70C8E8B3EE23403B00AFE7B21922341AE818736930826FA0BE82119B2ED203AF426FD79885048DE2672553ED77A2502A8173D18C2FCC37F7DC410BB048BED637E33A7DF17113F1E0279DF82C79A3F96053B6F3531D854C8506D2CB11723F9B7C490367A0CDDD1C9F89E1E4D96B59BA855F2E209623B6B7DA6C04D03B6D042F0B1CEEE96F20E97C5763DDF2B799CFB4C08CEFEB08C6E185",
                    "challenge": "4CF2071FF346FC324883455AA0E4363B0702A316623A4D8D3BC1211EF146E0F7",
                    "response": "546B79D7CD8A54804CA7CBCC5995A794F4307D2E04DC4F8AACE574CDF340B67A"
                },
                {
                    "public_key": "82A7140251072CF0EEB911373CFC41C26F5E8C0D09AC68AAB407D796580BDCFEE2F7E13CACECF82E0E0991FD7D01E61B1D86785450B0AEC8C3412F020741C63232E03299FDF5A73732079F70C8952CAFDF3412F78B08E5DF96B1BA353707C5D1B6E2ED3B7025FD5BFAE39E1A09C9E6C647A55488BCF5C4B496BEC35BA365F7E7F65B5BAF67D0ACFE4DF78D8D128FFA621E67C068BC2EA710BFD9FF6119220A53A5ABE335D6E71020B99730FE4A87A894C34F62126431AC09CFFA6EBDB05E52AB3E7C7379D06660E3DB3908E2020D48834D0E0E42BAA2B6C71119B266D5B9B6918FA91170BCE0C322B72000389E40A350B5483EE2B675DD9098792B32E609D2DF207F38C264CF1C73C562B545563AC735830F5B4BB0BF573E88A6DC851649A1B66B66E4D4BEF401037E8F1724883A9636AC4C096C3A1A208BED7A498E9530EF9F4212BB4A682D603B2D82AF397C751AC02B1E5335E80AD4C89865B5D59F3FA741AB41D54C01B87148A82124CFC6054EE2B7B6000DD8AD6727CDAC5BFFF5138D07146B5D1E4BB2E7D4FAD93420D7D02C419CA642C771F4AFF08A59CE96D3391B0EF73DCD5DD40F237D9CDB4E8FD725CCDBFF9CEA3D9D25E9E22989DABA3837416D046A5108D952E8382B89032D38EAD521FEB896D338271E1B76054DB4DB6923F201ADB593892C5DCF45BAF51D2A6446101427B99D05812A4423F159046053EC13",
                    "challenge": "C1270947D988FF593E27B68564475B44FFC1693DD9FFCC1D2DC3ACD7467F209C",
                    "response": "0B402614C2294BC81321EE28529248D7B7E1AD257A0E310112D614F99E022F34"
                }
            ]
        },
        {
            "guardian_id": "guardian2",
            "x_coordinate": 2,
            "coefficient_proofs": [
                {
                    "public_key": "508DC269D9F4A2049C0AC67C7E7F2292AB03A6027C9F3A620C462C7CDFFCF09DDFC6E7E9B9978B8722BE447DED0593E4B1261584BEB7DE78E3D2DB2629010D71325D3EBC1415B9358A00FCACC8450C12D3014B59E14DB1E0A1262A8C49E16C6B06C6BD9E6C5E0EE8A1E3BC99F2F5CF1D3D757F62E86AE94F993C75480A15FFA12A364CFBD14DDF490648B8A17FD8C94E5298399E6B0FC8526385AB185D05929012DC4F79327E651E4DFE3772514456F52B508E6166B98013A64E9A44D9E0C6D2A895928B1194BB1974F8FCB659515815AE1A851B96E51946790F193887B304BC11774FB5CD7042AF1D26DAD5DE0B0A7C9493BC39437EAE61097FE37A418CFB367DF8E5C8F584716B323E086E88E8F5D704184CEC22AEC9A575F84C75F9450B92AF64507E82D68ACC2F3C699B86C2E991B74A319F3E26A46BCCA2300E9751717382F69BEADB23B6C8BE3BE163F144351C2DD89D2903D588DE6DF3E0CCF668E879EB284E54B4BA124DFFDC920E24F1A5E57DECF37607B65CD11021DE7257335B1C7A33DAC6A78008B8D7DD773F039E8641528B5E49CFE9552563C83498B7732BAF28AF9DF548387573C0066090BD5B93087A47CE432FB9862A8F7BD83EA788E822C96226F1F7764863F7117E2471D6132E39896607949CA719D0D6CBB07B95F713C8D1F8E19133B1573B319775EDB047830026831AD26358DFD770BB0FEF00C08F",
                    "challenge": "21AAB78D77C7749B2B0F135A40D053E5C809A6816733627C1257E4BF6AEC57E0",
                    "response": "A6D3ED3183B199D0535D434055B37A07C8EE6C6BF9650317F2D14B2C844F0EC4"
                },
                {
                    "public_key": "05D9080C43679394637788D55DB494D745C93D79E36DB2E5970EACBE48AE1083EFCCAB7A8E33791D0F7AF10120F4E59627299FB158F92D31AA89E2DDC142F6241B8F6DCF3F596B03DA8A1364F068FB5586695B74576E8CBA8437FF8BF7DB6865FD6A43274D34DBDD7B268AF8A8F20145F4746E0BF0898EF8C4EE409DE5224704FA97B4E25E422AE2FA7B1FEF4708698DE9A14A62F9C9B35B9074CD3094CDC2F43DC8A35D1519D70E04448088A982B0300D747D586D327A8E7C5867CE6A42482DD281350CEF0F24124F1094BDDB2EB10444ADF8025A84594D252D202D864193617321F4E5793D932763222487C0361E3BC02868390EEA903F7D0E1AC76F1CCA39EC4EE5B51FF6DBA2BBD642CFF3B860473D1E8751725A0583DF34B8EB3025CC7718150FA71AF06AAA06247ABE4076B5C4BFA7EF545476EB559DC8B5405824F00950B3DAA515797E9A7CC6C51D4CABE597B57700C9CB15FFF25D27F16565EF235D72244E17296742628FF4AC7A243448374BD20C2DEC2AE62563B4F512587FC3739BB881C6997C5B8B0BD0EF6DBADAB9CB9D4040C8C3BB9E475B2CF6F30927507898F075BF279D27F66C59F880F5D6383FDC420D4BB97AF5A221BA70CE845AD23F041C0834EE4EC8180F23522F82930EA8CCFF40C8817AE3EEA1C456E25F38D05BCF6F432FAE816DDA39FEFBAA30B99C64720AF36D39D135F3ACF1A050D5D2979C",
                    "challenge": "5B5B4FA9508A2AE23BB0C167D0F831C68CBDE8991F82D25D912C39FF3AD0C7DB",
                    "response": "78B316A51EAD902C6B674AF4C975BA63869C49FD648B840FE6E644285B522905"
                },
                {
                    "public_key": "2D7C67637E27201A5B317192ABA9D46C94964FF6B96554F514026522A6EF09FF880BD7EA7AAE1F4BFCED04A2B7D7AF5C9B3C0A3DF39E5914DD76CF38F5F215C4D8ABF4571FA5BB558A318365E02B5531444D255350D8035FBC139E0E06CC2D08082BBD773C3D1042189EB35B682A4F08F35DBD72C3060953BACAA41A61A37B2936456BB968CC34049A9D07C6361579D93B1BE78F1B836D3976856720B461F1053C0D47931C80242D69DC149656029559B8C5A28E8C62949F2FC8F31B9AA6E9972F916BC2736E722FE6BAA5CD05FB04F9B3A62553571236BF1D76008D20FE64A763976A70C2AEA736F93B95EB5FF4B42572DB8BA0E898E512276FF0531BE6F1A1D8C2FEEDE9028699DFB34A2947C81E0D28704A6CD53D9913B75D4EA83BF9073B575AD916304EA040BA095EEF3920C179F22FE0AB8F32FC87CE09E92B21FD33CE24568D75E4E82A03DBC9D31445006E0A1576949C135DEF9EF4803974EC42D0B8FE8E64A196942154EE2499932CAF150FD257773509C0D6383AF26E79EC2E628E62BAA8A28093C806A10353F11919EF3E779ED89D4AF15F957FD83177DBC3496DB96EBDB27A707880A98D8C5CB7686EBF9DB38F9E61A7421FCCAB1FBDF5411104AE0254FCA6D71E3CC7AE1A4FE7EDD9450839D2D2BE72CB77004BC5EB0A8B14A6CFD86020CC84977411B7F84B95E126B5551EEBA7F8E345BF339BE6EA3765C246",
                    "challenge": "E56CC306CB297D8ED83961FBA47DD8AA030CE7EE93A154815B1F9A97FD873AB1",
                    "response": "1808B831E08F67238C8E86DC598E9B1BAA56268520DE141D3FE5181A74CD23B4"
                }
            ]
        },
        {
            "guardian_id": "guardian3",
            "x_coordinate": 3,
            "coefficient_proofs": [
                {
                    "public_key": "4947DA00D382DB38BC59B8CC07786AC94C3406348CE0275A9D831E9992C67D0032AA6D6E42F828FA8F9386856A17AFF01431D4F873BD853393EC697644AFDA5FDD5AD11833AC08BB56FFA6D7A0CFB55FE86B21A4A13176A40A1FB879D342FD99A68DCF75B5688E55AE065AA97309EBE6ED72A635A6AA576C6AF97BD32FC9439972636D0BFCCDFFD68692C6266002CCA27EA5E8382847528F0E126D48BE59CE09AFDBEF0BDE537EE18E71AA4598ADF1DC99A0B475E622F6214D79FF07B133B981AE97276D8DFD22D92EEFF330FA07E5766D37CC12986F52FC4545A76680F80DD768E2B871DA5403CB7CF55EDA21FBD075B8226406D9EA1B2FCB43C69AA29226DE5AF63886D72A37367ECF88F7017D322FEAA5DC743819B80BF30500B4332A18EA373BE7D00A1D4D1449BE0B92FD09D40563DD6D7001A53A44E8A85630B2D14C64845120A205FFC793B548B56285A0D01B3650DE93425A73A1D2C33147875DCA2609C2CF864DEA240AEF249CB179F206F8819A261213E6D3D237C4C5E781A61E6231A71ED1CD0E8F75580DF094739F37C0ACE33B832FF436F90AB3A45D5F9717D0C35A0821AA35C54E2E1CF6DC7EEBF514D99760FF64D6974414EBE0890294100E2A369C667EBFC40F77B95C267633DBE8C3C02AAFC8C1F69369AC2D87838D76844CFAD0357906C374FB91D34A93A4513968F989B5E17A63BC317D3D7696980CB3",
                    "challenge": "EED35EB085F3682CE579A7E76893318B3F44CB00E31658E28CCA89B281DFAAB5",
                    "response": "688EA773A790A85BB1D73154F48D0C9B13A27A7D8BFB7DA4532F7EA991771DBC"
                },
                {
                    "public_key": "7D4471529072CBE49BBA2621F5A7C6D25FD3ACB5A08320A0E9FFE3B3EDE800FD27F0A5D168A85E595B833ABA2175953881EA55C234FEDA396F6B9C1B1493FF815FA241C69FEF9A72FCD38E0AEF95136431502D155994E3BF0184A9254606A2562268B83D99CAF9C6E6BD3F8E4879FD23DAF8EA1C3219143BF60D1AE94637C40A07C07B251A58BB53BCBAD0AA08E7F4124D9204FD7E4F69425D22FBAFB22CAB4E518BE7E1E7F7C12058E6E60CFCDE5CEDAB69EF9C166EC2CA8A9D305EC234F940A038BB3DE9687EF9BF195ABD40005DE3736A77D4521AB5A378A62C8814B9F7F66D82440B9F48E07E8FF95FBF176204BD4DB445F81257EF5B078B34742AE48D6EAD4FDF5C6D4DCF1077ABEE1094741E42C22BB0394197136E16C80493B2BDDD978229B9C874BC3E5DB31F60F299119F541C336094AF0C87BDE7E83FBDD30C59EC9431FCF822EE655CFF6E2582284EC64C103CB37A5DB4965149EB4944D6923878FF1D8CE94BF7D54308116C4A1CD797347D44A9E8D09E33A02634306D477F203251500901E0C7618F233F29BF58982E4FACCD6E5B695ECDAA33ED4724A722E8EE1EA0F415390B244956EB59B97A130EA07B927F4EAD3DE62ECF52A6828CB99C3B66CC81A325A7E1C9299410F74AFC3CA537EA2470471ED984CAE946A3F03A7A5D4F1E66B07CEF859AD6858BF57A56EDC35BE34CA46E0FC55E43DB5A907EC5E0C7",
                    "challenge": "DD8FFBC570898A36C2DC8A6642C21FF4FC62B2FA69014132450C11291089B8C6",
                    "response": "7AE64FF6338C5D81C6F298D97CED67AB69FB39E70FB622541861E6992D1E87C3"
                },
                {
                    "public_key": "76E2C56269E039D13B9E49E642A50C8B7FB21490A01E57AED8513E0DDE766C6F41662BAA014701132826612E5F5F74933C110E1B0C828062EA10B97EF3B461704459B0108A358166C408D3F4EFC694EB150B4793BD010BC3BB760CA67F00316501FFF72D8A94553E7938E91C6D00E826542AFC59E589C2CD960B29B0AA6C4DD85ADB5F3155AD641A476F38EC98CAA0612717B277BF0610245821B8798C5AE521057F05AA7B249263055DBB3828911DC2B065F0319AEDD647CDE8ED24113BED4C2B0E902306375884108DF30591A49D14276A67321FDDD78E08A6B0EFF5AD9B5F37C355E7D4375B5884740EBA3846F1277C4B8409D3E2C8FEB40F618C1F72FAD7CC6E4127261B37AA5D4559690502EC4EB3986A1E8BAF46D3D8DCDFEC35E5A566DBA4C932C6A4533EED927F5C45B94AABDD94867497AAB1E13DA5B110096878157347E05A5D724B70FD85BDCBC03635ED498380AF8AFE4A735BEDB62F56181B8A7B234A02D2E50D4824D19833E2DC5155F279A9C59D7D4BA057EA3EDD7105D189BF2B0DEA3AB9285D76D49372D8B244526E3A87E154BF4DB754A1CE3F628AE6DB92D2EB4D4F62F2CBC5E1646F16AA4178CBF0E59A8A83D9D92E82E73207DCF1089728D9E174D0D20D312A6240E2E325ABF53FE374EAD43BF045E729B79709BE87C28A2AA98F685357E3058BF63CE5C5996E39789AA33C184EC76FB5DCB1806447",
                    "challenge": "6CE3EEE2D6D9D8811B7A8E44DD3A1CF5BA7FC6D6CAF0CF655CCA8EEE0BAB96D3",
                    "response": "E1CC9C9AE7BE437EA59A90646C213D1FE670A7EE1A18FEA1651C72696FB28EB0"
                }
            ]
        },
        {
            "guardian_id": "guardian4",
            "x_coordinate": 4,
            "coefficient_proofs": [
                {
                    "public_key": "5050275900FA8F5DDBFBCC42F866F217B71C073757E85F5D80B3A2CA5319ADD3EE5CFF1F9BE3141AEB805450451C1381AF649911CD2CEF86E8D02AA83CB009094A3EF85E7A8D14344D103A6EFBB385B09AAD6119EC1DE3BEABCDB7B91C66E7E64931B388BA372333DE05C9E12D8A183392FFEA040E2CBFAF013F4186D70C4CE982F5C2285EE8571FEFD221FA653EC5ABFC7E0AEA59EA0C3B101AAADD48F6DED634850C9E7B908880513B6DD3654DB868B53F26D483CBE4177A12B7D6207C5D1432A708AD51993FA10F5D64BFEF5EF2621AD09EE25A49DC766244ADD8180D70415287553F0B014198DCE724E60A13318A1516B1B8979C63A1C550F572F313A80B2A29E051A382B034B2E64B12C6DFC57274167E6CFA9BDDF59D7EA60CC987480CCFAFCA9AF2B0D1B023A1A25182A1E7CBC5F3A0B55B7027E0E3A3F33057C6C478A741E7466A8A7583FAAEE8454A90501104A7F5DAD39FEA4E523F9A7C5F0E4C03123A0B372241DDA3534759B8890C88297EF876AADE16076D3234EC3F0CC3A95C4C16EF4D15013C6018A2BD2A43F23478893F9970C0A1DF97393F8A2F7CFD1C7B8695D95F22176C2F2857A349184CCAE2D9AEC966C734BB26F78AA5EC075472D93972811F528F7B6A6D00CB53330F6884FF2A27ECBFD4136C422B8DC48C15CE023C5F1B7B5B37C561135320F4D0D7806ABB53DEAF3A08D65BE33F7493C42AE199",
                    "challenge": "8DD40662073E8A9DC8AF2617D8EF914642DDDA48AE49411B968EF220EC2AD832",
                    "response": "A05B6EE49875F48669E9286DDAAB395F49341077BD5636762FB98DB1EB3BC36C"
                },
                {
                    "public_key": "B8FB8056E582BAA7DBEED7F93098EADDE8E6C9A094FD5EB942E79874AC639885519586CBF26E03EDA4ABE130C651116A97AD78573F0B078AFA82166071E6CF50D85B72DFF4ABC930E3CB5FCA0343511D775F6242389FBCB0A8463D8DC8728F5A7D6747B0BFF858A9407C693DD423D2E306AF8DBED65060434C8723BA65567F9C0C7E0718B4BA41F15033D2273D0AA43CA1CCB9A5DAC9F3D58AC3589A51DECF75FA02C8BFFB8BF14811D0E8E4CBD6F63196542CF1CF29D2FFDD439DD5EDE943C20A564D477FCD82C5CE2675017841C6BF729B0FCAB98B31474AB2096DB669C04E5A5C278ECB8925E504920E404B633A742C2A0559C8D7210994D3F1210128981480A5E036FA549354C2B0D35A0315A8F37AE3892B85452512394A1CB4495F1DA9277443EEF5A5B521D737ABFEC69BA20DC0EBD276652280A3C3C4A20A91005B80B29065802F8FE391F67577A770D7E3A8DEDCDBF3FD0FA221B31495E3BF779096D7D8FB47D6EFBB9CFF30A7895B0153CB272A5B62C02B5EDB80955870B165886403786492D4922BAAD5BE0D981FA630BF742F779ABC497D841E98CF84C23C0222E101590C25D04C808E85707874FF7DD1F8F1A8AEE01AE82AF8EB8F75742BE65766055CBCFEC84798BC29032AFB631BB8C5AF0593BA31E148B52543169F08DC6394401E6D9CF3D0786805411B7EE6668BB1B2FC49874B6B3C1A7E2F0B6929D6C0",
                    "challenge": "4A4677D95AEF8F8A4CCD64B9C38C5CAC4497AA5ACE870DE244534552AE5896CC",
                    "response": "C733858E04CFBEEF0EC8914C90B11B57F6F426A89906DB05FF279DE9B8310C53"
                },
                {
                    "public_key": "C0E6ACAE72B55599E987C51A34581AA926AC02148D010A1DD79222488FA0A294554941E8EB081383BF132EEA85DF2E882F6CAC286D60163BC711C77F9D7B31F730F6D2DCCA802626A190C6394F122CFAC3CF5924B2BB7B1E018BD69FFA2B10426E224452B431D08B739E74DFA921EBB90E1514723A2F6B9CCFAC3AA9C11EBCAD9049A8A76504686CAC7A8427A372CB1F48EEA29BD7BFB5E166897384493DFCA58B31B164490010B6CD6F65E7AA1B4E8613D9BC35F480582E2440C39758DE4DB99EEAAB2A28AB79B429F549CD19D194E74A4A43028E24CCB4BA92F67F203EF333B197A8502D79428DBBEF265B4ADC259862DCBB46D59B0F61A7BC05DF1446DB86700365F043430F20CBECA45AEE2445BD49B547796D7BB762D9521881949438E431B6B8FE7A4EC3EDA318EEEA2349349E3CCD2825217373258FAAF46CEF0F386D17E047B36535EFE9B16B7E67C45B7236088B29CBBE1F8886B43B5B2DA13C271DC183236361C374A2401D73BC65FE17C894E229F5A4EF8E5AE5336E35F662D2B7A9AF4AA85898D0FE5EE7CDE00A21F3C4C6500D31BF79D1FB0BFD6ADFC6EAF2F255BB6B2CAD3525CEC65CB334256E90EC5D3B789394C27BD312BDFF1533B6966832E0F3FC752CAF2E3821D15DC0C08A0B7733FB0C369E18D9EEB2CFD85DBC86ABAAD0EDCF8FAB484DF681917A3D765B991E4C816FCB627D2FF0B0F221AD63D706",
                    "challenge": "64F0F20D802491EB326D8003649B99BDE96F391AF860F7BF7E0AEBADE726CC5B",
                    "response": "605D190AB4908915C47CBE1FA6E1E9C76D20D12BE80391D1573A956BABC602C4"
                }
            ]
        },
        {
            "guardian_id": "guardian5",
            "x_coordinate": 5,
            "coefficient_proofs": [
                {
                    "public_key": "391591AAA3BD7CB8A22B614906C3CF5A143F7768242BFDB823953F18F12859E7FD4EF24E2F4142584ECEE1B8D8CFAB0C63C3697CE4026E863D907A0DA9CEC69DEB1704138D90B5A39A01996935245137527BFA1F7CAB2AFD27B2277D2AE219BFAF62E78E6C564A78556471AC531E243CCE0F4E16F7E5064A4538CB25996AE7D11393563340F7F94F77647B7739CBCD24982317A959910FFD742D0F1163B562AE0BC519AFAF5CDF82DEEF78FF94D1E0342754C760580A07D8B87A3B711CC66D1EFE6EA2E748FE858614406C41168DC164DB005AD7C3E7D92C5C41F4268B908A3E5975063C32A48CE1834CBDC5832D48162672C06D7F96A3C4258D35090A4197E36C3B0141DAF36D45F2AC3CCE2C060BFCEB0F9D2D218872AD2E47AD03BD4A3E47A32CB9AFB15055D2497709F7C9D3E60DC5636C5F7EFD324444EA0F0919EEBB60DB6A3C49CF9267AFD6879954C59BE8D73E0E51A7B487312D91863407E5296DD11700623BE0BAFBBA1376AA38DAC096EF3D422AE2803D89A7656B99211E405827E2F924F0D375AC6A8ECFC95C30829D0C6345299708E691674A188FD4A40B4B2D45F85524EEBAFDADFABAE2799B486D5C8A8EF074D06240DD4A66636281F894E1426BBED90AEAB16D1391465F526AA3CB21F9864A2C11B3EAE0EA2CE21F005BE75C2BD42817F1E5B71789B1E51EE7956D2D543A973E2A72D091530B636D00EDC2",
                    "challenge": "B7FA04F14A2CB560884AE50226D03B5135AA15BB7797333188A6BAA19A13D646",
                    "response": "011C23BE145E47A27E38F0C3595648945176042BD2617BFEE29C37E9D93557F3"
                },
                {
                    "public_key": "A7961B1781E7AF377E43C61FAE5BDAAB9ADEF7A638883BFC59F10D7E3A18A3A45F7D522916F229E5AF30F62E7E0CF564874CC5C3A5707C0C1B70C703071941A6AE981E4C1EDDA03F314EE9952DD4B50BDD3D3FCF8E4F0AEC005C5955FAEC1A4BBE11DF174A52B3D8CF2DB8C3A65D417143CAA87DC35BA802D92EEC16FB35BCDDA1C111851D317A20F88411B975CE81896C3658E807E667EB7C2ACAB60B97413D7F824A9E4E73FC922B870FF59AD0FED093C741384FF0B10842B10BC8019660B708E5C09177972957290DF2D9E985510A3985C775B11A994643767CEEE1087CDF404978D1D11C98671F47D0875B7D77F2601E65E1D316F4EAC63BADACE1230C240AFF8AC9C173B025542AF722AC85648EA436B52C377FE283F9E6084257F14C96D4F4C980FE046925731F62228726D4A1D1ED0F9604BBBAB274AC170FC1BB4CF59277EB416F2710C9FC743F6101E725274E633C913925973A1F85E0D1F6E6E6106726FA8DC9A67442726526B3C936D328082CA0E9D76A3BE9520C944F2C6D7C94103D3F0F6DFF4239A2338CB562DDB7B56F30A6F10D4793912B24C72488ED29472B5B2222566FBCAF11BDE729B5D617CB910C04B42659F1C9712C5C71A80EF06DD2E1CA14448C62D0B47283D5C8BC20DB5B1E708A7AB7EDB3AB329029623FF36E28718B3862F80B3AC282687128AC775221FD4CE40474AA90EFAD29FFCCA9338B",
                    "challenge": "E2A3280FBC492280329F6F46D343168E196AA92211677A5A2A30C74E50EC236B",
                    "response": "67966F3A55CC4A4EFA12B0E72E1351BA400216CC582EE4BB5F3F5D8F99D5F0E3"
                },
                {
                    "public_key": "AFBE30232C3B79CAE7E9154EDEBBC40DF43A28606B8E8F64874B684152EFEDB1C2BAF00C99C70059ADD676739B3C260C509A7D24127F0E2A890C3A259BD3C1EB04AA45A6F89196B4319BEB10AD6CDB99704213F1B6FDEF6FF1E55BCE38FBE63F32B2F18C74E0066606E665D2A9DF0CF2B4A6904788A0BCDCDAD244FE8F1594DF8D78FF2C3E694BDF5BAC6D019028F43EBFAF4DB11D15BF04848DD60882CEF3A73C9F7BDF3B0B07F585B50E4B50D95C76C91A9DFFE4A96CE9678D57E03A51951360ECF4B081740C3611450A6D3090358DADA2C92AD44810D003C826CD43A62822528EB2ED4547233E0105FF916B135FE1CDF52D71B7B6F99798CE5AF9C1F3A387738C60E9EB4994CEA25E426433F50E324AC99F921FC16C3FC3CF65A2CD0006BE4423E1089FD2030110FD2C5FEFF19FCD3B2D1493D4410ABCCACE06C6B849EF624544EE2454F555F9439EF219294EFD974B727BA460B58873AEDF58D9CEB46472C81767885CC3A9E0F9C4EFBBAB7B7DA9B8A8B2D87E6E11D4A7ECED5E2C4F625CBB9F151109A252E95D05507CDB0EABF2C0D6BA310723D480CF7E89F12AD45A00BF9FDF0EF454508DEB6C8BA1B0666C6AE7D10FD8ACC74C0196E1FF3909E8FB283DD90AD2D5FE5A30FCB5E4ED09A299E95707E93B52B80012EA40B6E601795089B269BF589A0567DFEEC6A2323BBDAE8F432F5A85F8D9B11F6D21416693B4FB8F",
                    "challenge": "4316CBB1170CED9F8CC8BC6AE70BB20B3B461D8FD3F186A223E00095FA2520A0",
                    "response": "595B5354CBFAFD4E92B0CC1087EADFB7233131FDA54D11CD6BCD4BCF98D5BE8C"
                }
            ]
        }
    ]
}
````

* The number of guardians is from the electionConfig
* The number of coefficient == the quorum from the electionConfig


TODO


#### message ElGamalCiphertext

| Name | Type        | Notes   |
|------|-------------|---------|
| pad  | ElementModP | g^R     |
| data | ElementModP | K^(V+R) |

#### message ChaumPedersenProof

| Name      | Type        | Notes |
|-----------|-------------|-------|
| challenge | ElementModQ | c     |
| response  | ElementModQ | v     |

#### message HashedElGamalCiphertext

| Name | Type        | Notes |
|------|-------------|-------|
| c0   | ElementModP |       |
| c1   | bytes       |       |
| c2   | UInt256     |       |

#### message SchnorrProof

| Name       | Type        | Notes |
|------------|-------------|-------|
| public_key | ElementModP | K_ij  |
| challenge  | ElementModQ | c_ij  |
| response   | ElementModQ | v_ij  |

#### message UInt256

| Name  | Type  | Notes                                           |
|-------|-------|-------------------------------------------------|
| value | bytes | unsigned, big-endian, 0 left-padded to 32 bytes |

## manifest.proto

[schema](../egklib/src/commonMain/proto/manifest.proto)

#### message Manifest

| Name                | Type                       | Notes                        |
|---------------------|----------------------------|------------------------------|
| election_scope_id   | string                     |                              |
| spec_version        | string                     | the reference SDK version    |
| election_type       | enum ElectionType          |                              |
| start_date          | string                     | ISO 8601 formatted date/time |
| end_date            | string                     | ISO 8601 formatted date/time |
| geopolitical_units  | List\<GeopoliticalUnit\>   |                              |
| parties             | List\<Party\>              |                              |
| candidates          | List\<Candidate\>          |                              |
| contests            | List\<ContestDescription\> |                              |
| ballot_styles       | List\<BallotStyle\>        |                              |
| name                | Language                   | optional                     |
| contact_information | ContactInformation         | optional                     |

#### message BallotStyle

| Name                  | Type           | Notes                                 |
|-----------------------|----------------|---------------------------------------|
| ballot_style_id       | string         |                                       |
| geopolitical_unit_ids | List\<string\> | GeoPoliticalUnit.geopolitical_unit_id |
| party_ids             | List\<string\> | optional matches Party.party_id       |
| image_uri             | string         | optional                              |

#### message Candidate

| Name         | Type   | Notes                           |
|--------------|--------|---------------------------------|
| candidate_id | string |                                 |
| name         | string |                                 |
| party_id     | string | optional matches Party.party_id |
| image_uri    | string | optional                        |
| is_write_in  | bool   |                                 |

#### message ContactInformation

| Name         | Type           | Notes    |
|--------------|----------------|----------|
| name         | string         | optional |
| address_line | List\<string\> | optional |
| email        | List\<string\> | optional |
| phone        | List\<string\> | optional |

#### message GeopoliticalUnit

| Name                 | Type                   | Notes    |
|----------------------|------------------------|----------|
| geopolitical_unit_id | string                 |          |
| name                 | string                 |          |
| type                 | enum ReportingUnitType |          |
| contact_information  | string                 | optional |

#### message Language

| Name     | Type   | Notes |
|----------|--------|-------|
| value    | string |       |
| language | string |       |

#### message Party

| Name         | Type   | Notes    |
|--------------|--------|----------|
| party_id     | string |          |
| name         | string |          |
| abbreviation | string | optional |
| color        | string | optional |
| logo_uri     | string | optional |

#### message ContestDescription

| Name                 | Type                         | Notes                                         |
|----------------------|------------------------------|-----------------------------------------------|
| contest_id           | string                       |                                               |
| sequence_order       | uint32                       | unique within manifest                        |
| geopolitical_unit_id | string                       | matches GeoPoliticalUnit.geopolitical_unit_id |
| vote_variation       | enum VoteVariationType       |                                               |
| number_elected       | uint32                       |                                               |
| votes_allowed        | uint32                       |                                               |
| name                 | string                       |                                               |
| selections           | List\<SelectionDescription\> |                                               |
| ballot_title         | string                       | optional                                      |
| ballot_subtitle      | string                       | optional                                      |

#### message SelectionDescription

| Name           | Type    | Notes                          |
|----------------|---------|--------------------------------|
| selection_id   | string  |                                |
| sequence_order | uint32  | unique within contest          |
| candidate_id   | string  | matches Candidate.candidate_id |

## election_record.proto

[schema](../egklib/src/commonMain/proto/election_record.proto)

#### message ElectionConfig

| Name                 | Type                   | Notes     |
|----------------------|------------------------|-----------|
| spec_version         | string                 | "v2.0.0"  |
| constants            | ElectionConstants      |           |
| manifest_file        | bytes                  |           |
| manifest             | Manifest               |           |
| number_of_guardians  | uint32                 | n         |
| election_date        | string                 | k         |
| quorum               | uint32                 | k         |
| jurisdiction_info    | string                 | k         |
| parameter_base_hash  | UInt256                | Hp        |
| manifest_hash        | UInt256                | Hm        |
| election_base_hash   | UInt256                | He        |
| metadata             | map\<string, string\>  | arbitrary |

#### message ElectionConstants

| Name        | Type   | Notes                             |
|-------------|--------|-----------------------------------|
| name        | string |                                   |
| large_prime | bytes  | bigint is unsigned and big-endian |
| small_prime | bytes  | bigint is unsigned and big-endian |
| cofactor    | bytes  | bigint is unsigned and big-endian |
| generator   | bytes  | bigint is unsigned and big-endian |

#### message ElectionInitialized

| Name               | Type                  | Notes     |
|--------------------|-----------------------|-----------|
| config             | ElectionConfig        |           |
| joint_public_key   | ElementModP           | K         |
| extended_base_hash | UInt256               | He        |
| guardians          | List\<Guardian\>      | i = 1..n  |
| metadata           | map\<string, string\> | arbitrary |

#### message Guardian

| Name               | Type                 | Notes                                 |
|--------------------|----------------------|---------------------------------------|
| guardian_id        | string               |                                       |
| x_coordinate       | uint32               | x_coordinate in the polynomial, ℓ = i |
| coefficient_proofs | List\<SchnorrProof\> | j = 0..k-1                            |

#### message TallyResult

| Name            | Type                  | Notes               |
|-----------------|-----------------------|---------------------|
| election_init   | ElectionInitialized   |                     |
| encrypted_tally | EncryptedTally        |                     |
| ballot_ids      | List\<string\>        | included ballot ids |
| tally_ids       | List\<string\>        | included tally ids  |
| metadata        | map\<string, string\> |                     |

#### message DecryptionResult

| Name                 | Type                       | Notes |
|----------------------|----------------------------|-------|
| tally_result         | TallyResult                |       |
| decrypted_tally      | DecryptedTallyOrBallot     |       |
| metadata             | map<string, string>        |       |

#### message LagrangeCoordinate

| Name                 | Type        | Notes                             |
|----------------------|-------------|-----------------------------------|
| guardian_id          | string      |                                   |
| x_coordinate         | string      | x_coordinate in the polynomial, ℓ |
| lagrange_coefficient | ElementModQ | w_ℓ, see 10A                      |

## plaintext_ballot.proto

[schema](../egklib/src/commonMain/proto/plaintext_ballot.proto)

#### message PlaintextBallot

| Name            | Type                           | Notes                              |
|-----------------|--------------------------------|------------------------------------|
| ballot_id       | string                         | unique input ballot id             |
| ballot_style_id | string                         | BallotStyle.ballot_style_id        |
| contests        | List\<PlaintextBallotContest\> |                                    |
| errors          | string                         | optional, eg for an invalid ballot |

#### message PlaintextBallotContest

| Name           | Type                             | Notes                             |
|----------------|----------------------------------|-----------------------------------|
| contest_id     | string                           | ContestDescription.contest_id     |
| sequence_order | uint32                           | ContestDescription.sequence_order |
| selections     | List\<PlaintextBallotSelection\> |                                   |
| write_ins      | List\<string\>                   | optional                          |

#### message PlaintextBallotSelection

| Name           | Type   | Notes                               |
|----------------|--------|-------------------------------------|
| selection_id   | string | SelectionDescription.selection_id   |
| sequence_order | uint32 | SelectionDescription.sequence_order |
| vote           | uint32 |                                     |

## encrypted_ballot.proto

[schema](../egklib/src/commonMain/proto/encrypted_ballot.proto)

#### message EncryptedBallot

| Name              | Type                           | Notes                            |
|-------------------|--------------------------------|----------------------------------|
| ballot_id         | string                         | PlaintextBallot.ballot_id        |
| ballot_style_id   | string                         | BallotStyle.ballot_style_id      |
| confirmation_code | UInt256                        | tracking code, H_i               |
| code_baux         | bytes                          | B_aux in eq 96                   |
| contests          | List\<EncryptedBallotContest\> |                                  |
| timestamp         | int64                          | seconds since the unix epoch UTC |
| state             | enum BallotState               | CAST, SPOILED                    |
| is_preencrypt     | bool                           |                                  |

#### message EncryptedBallotContest

| Name                   | Type                               | Notes                             |
|------------------------|------------------------------------|-----------------------------------|
| contest_id             | string                             | ContestDescription.contest_id     |
| sequence_order         | uint32                             | ContestDescription.sequence_order |
| contest_hash           | UInt256                            | eq 58                             |                                                                     |
| selections             | List\<EncryptedBallotSelection\>   |                                   |
| proof                  | ChaumPedersenRangeProofKnownNonce  | proof of votes <= limit           |
| encrypted_contest_data | HashedElGamalCiphertext            |                                   |
| pre_encryption         | PreEncryption                      |                                   |

#### message EncryptedBallotSelection

| Name           | Type                              | Notes                               |
|----------------|-----------------------------------|-------------------------------------|
| selection_id   | string                            | SelectionDescription.selection_id   |
| sequence_order | uint32                            | SelectionDescription.sequence_order |
| encrypted_vote | ElGamalCiphertext                 |                                     |
| proof          | ChaumPedersenRangeProofKnownNonce | proof vote = 0 or 1                 |

#### message ChaumPedersenRangeProofKnownNonce

| Name  | Type                       | Notes |
|-------|----------------------------|-------|
| proof | List\<ChaumPedersenProof\> |       |

#### message PreEncryption

| Name                 | Type                    | Notes                                           |
|----------------------|-------------------------|-------------------------------------------------|
| preencryption_hash   | UInt256                 | eq 95                                           |
| all_selection_hashes | List\<UInt256\>         | size = nselections + limit ; sorted numerically |
| selected_vectors     | List\<SelectionVector\> | size = limit ; sorted numerically               |

#### message PreEncryptionVector

| Name            | Type                      | Notes                                              |
|-----------------|---------------------------|----------------------------------------------------|
| selection_hash  | UInt256                   | eq 93                                              |
| short_code      | String                    |                                                    |
| selected_vector | List\<ElGamalCiphertext\> | Ej, size = nselections, in order by sequence_order |

## encrypted_tally.proto

[schema](../egklib/src/commonMain/proto/encrypted_tally.proto)

#### message EncryptedTally

| Name     | Type                          | Notes |
|----------|-------------------------------|-------|
| tally_id | string                        |       |
| contests | List\<EncryptedTallyContest\> |       | 

#### message EncryptedTallyContest

| Name                     | Type                            | Notes                             |
|--------------------------|---------------------------------|-----------------------------------|
| contest_id               | string                          | ContestDescription.contest_id     |
| sequence_order           | uint32                          | ContestDescription.sequence_order |
| selections               | List\<EncryptedTallySelection\> |                                   |

#### message EncryptedTallySelection

| Name            | Type              | Notes                                                 |
|-----------------|-------------------|-------------------------------------------------------|
| selection_id    | string            | SelectionDescription.selection_id                     |
| sequence_order  | uint32            | SelectionDescription.sequence_order                   |
| encrypted_vote  | ElGamalCiphertext | accumulation over all cast ballots for this selection |

## decrypted_tally.proto

[schema](../egklib/src/commonMain/proto/decrypted_tally.proto)

### message DecryptedTallyOrBallot

| Name     | Type                     | Notes               |
|----------|--------------------------|---------------------|
| id       | string                   | tallyId or ballotId |
| contests | List\<DecryptedContest\> |                     |

### message DecryptedContest

| Name                   | Type                       | Notes                            |
|------------------------|----------------------------|----------------------------------|
| contest_id             | string                     | ContestDescription.contest_id    |
| selections             | List\<DecryptedSelection\> |                                  |
| decrypted_contest_data | DecryptedContestData       | optional, ballot decryption only |

### message DecryptedSelection

| Name            | Type               | Notes                             |
|-----------------|--------------------|-----------------------------------|
| selection_id    | string             | SelectionDescription.selection_id |
| tally           | int                | decrypted vote count              |
| k_exp_tally     | ElementModP        | T = K^tally, eq 65                |
| encrypted_vote  | ElGamalCiphertext  | encrypted vote count              |
| proof           | ChaumPedersenProof |                                   |

### message DecryptedContestData

| Name                   | Type                    | Notes                                     |
|------------------------|-------------------------|-------------------------------------------|
| contest_data           | ContestData             |                                           |
| encrypted_contest_data | HashedElGamalCiphertext | see 3.3.3. matches EncryptedBallotContest |
| proof                  | ChaumPedersenProof      |                                           |
| beta                   | ElementModP             | β = C0^s mod p                            |

