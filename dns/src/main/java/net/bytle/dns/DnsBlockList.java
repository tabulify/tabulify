package net.bytle.dns;

import java.util.Set;

public enum DnsBlockList {


  /**
   * The list of <a href="https://www.dnsbl.info/dnsbl-database-check.php">...</a>
   */

  all_s5h_net("all.s5h.net", DnsBlockListType.IP, Set.of()),
  B_BARRACUDACENTRAL_ORG("b.barracudacentral.org", DnsBlockListType.IP, Set.of()),
  bl_spamcop_net("bl.spamcop.net", DnsBlockListType.IP, Set.of()),
  blacklist_woody_ch("blacklist.woody.ch", DnsBlockListType.IP, Set.of()),
  bogons_cymru_com("bogons.cymru.com", DnsBlockListType.IP, Set.of()),
  cbl_abuseat_org("cbl.abuseat.org", DnsBlockListType.IP, Set.of()),
  combined_abuse_ch("combined.abuse.ch", DnsBlockListType.IP, Set.of()),
  db_wpbl_info("db.wpbl.info", DnsBlockListType.IP, Set.of()),
  dnsbl_1_uceprotect_net("dnsbl-1.uceprotect.net", DnsBlockListType.IP, Set.of()),
  dnsbl_2_uceprotect_net("dnsbl-2.uceprotect.net", DnsBlockListType.IP, Set.of()),
  dnsbl_3_uceprotect_net("dnsbl-3.uceprotect.net", DnsBlockListType.IP, Set.of()),
  dnsbl_dronebl_org("dnsbl.dronebl.org", DnsBlockListType.IP, Set.of()),
  dnsbl_sorbs_net("dnsbl.sorbs.net", DnsBlockListType.IP, Set.of()),
  drone_abuse_ch("drone.abuse.ch", DnsBlockListType.IP, Set.of()),
  duinv_aupads_org("duinv.aupads.org", DnsBlockListType.IP, Set.of()),
  dul_dnsbl_sorbs_net("dul.dnsbl.sorbs.net", DnsBlockListType.IP, Set.of()),
  dyna_spamrats_com("dyna.spamrats.com", DnsBlockListType.IP, Set.of()),
  http_dnsbl_sorbs_net("http.dnsbl.sorbs.net", DnsBlockListType.IP, Set.of()),
  ips_backscatterer_org("ips.backscatterer.org", DnsBlockListType.IP, Set.of()),
  ix_dnsbl_manitu_net("ix.dnsbl.manitu.net", DnsBlockListType.IP, Set.of()),
  korea_services_net("korea.services.net", DnsBlockListType.IP, Set.of()),
  misc_dnsbl_sorbs_net("misc.dnsbl.sorbs.net", DnsBlockListType.IP, Set.of()),
  noptr_spamrats_com("noptr.spamrats.com", DnsBlockListType.IP, Set.of()),
  orvedb_aupads_org("orvedb.aupads.org", DnsBlockListType.IP, Set.of()),
  pbl_spamhaus_org("pbl.spamhaus.org", DnsBlockListType.IP, Set.of()),
  proxy_bl_gweep_ca("proxy.bl.gweep.ca", DnsBlockListType.IP, Set.of()),
  psbl_surriel_com("psbl.surriel.com", DnsBlockListType.IP, Set.of()),
  relays_bl_gweep_ca("relays.bl.gweep.ca", DnsBlockListType.IP, Set.of()),
  relays_nether_net("relays.nether.net", DnsBlockListType.IP, Set.of()),
  SBL_SPAMHAUS_ORG("sbl.spamhaus.org", DnsBlockListType.IP, Set.of()),
  singular_ttk_pte_hu("singular.ttk.pte.hu", DnsBlockListType.IP, Set.of()),
  smtp_dnsbl_sorbs_net("smtp.dnsbl.sorbs.net", DnsBlockListType.IP, Set.of()),
  socks_dnsbl_sorbs_net("socks.dnsbl.sorbs.net", DnsBlockListType.IP, Set.of()),
  spam_abuse_ch("spam.abuse.ch", DnsBlockListType.IP, Set.of()),
  spam_dnsbl_anonmails_de("spam.dnsbl.anonmails.de", DnsBlockListType.IP, Set.of()),
  spam_dnsbl_sorbs_net("spam.dnsbl.sorbs.net", DnsBlockListType.IP, Set.of()),
  spam_spamrats_com("spam.spamrats.com", DnsBlockListType.IP, Set.of()),
  spambot_bls_digibase_ca("spambot.bls.digibase.ca", DnsBlockListType.IP, Set.of()),
  spamrbl_imp_ch("spamrbl.imp.ch", DnsBlockListType.IP, Set.of()),
  spamsources_fabel_dk("spamsources.fabel.dk", DnsBlockListType.IP, Set.of()),
  ubl_lashback_com("ubl.lashback.com", DnsBlockListType.IP, Set.of()),
  ubl_unsubscore_com("ubl.unsubscore.com", DnsBlockListType.IP, Set.of()),
  virus_rbl_jp("virus.rbl.jp", DnsBlockListType.IP, Set.of()),
  web_dnsbl_sorbs_net("web.dnsbl.sorbs.net", DnsBlockListType.IP, Set.of()),
  wormrbl_imp_ch("wormrbl.imp.ch", DnsBlockListType.IP, Set.of()),
  xbl_spamhaus_org("xbl.spamhaus.org", DnsBlockListType.IP, Set.of()),
  z_mailspike_net("z.mailspike.net", DnsBlockListType.IP, Set.of()),
  /**
   * We encourage applications to query
   * zen.spamhaus.org and then parse the return code(s)
   * to determine whether to block an IP, whenever possible.
   * <a href="https://www.spamhaus.org/zen/">...</a>
   * <a href="https://www.spamhaus.org/faq/section/DNSBL%2520Usage#200">Status code</a>
   */
  ZEN_SPAMHAUS_ORG("zen.spamhaus.org", DnsBlockListType.IP, Set.of(
    "127.0.0.2",
    "127.0.0.3",
    "127.0.0.4",
    "127.0.0.5",
    "127.0.0.6",
    "127.0.0.7",
    "127.0.0.10",
    "127.0.0.11"
    )),
  zombie_dnsbl_sorbs_net("zombie.dnsbl.sorbs.net", DnsBlockListType.IP, Set.of()),
  /**
   * Spamhaus DBL is a domain DNSBL.
   * It may be used to identify URL domains with poor domain reputation,
   * or as a "Right Hand Side Block List" (RHSBL) for email addresses.
   * <a href="https://www.spamhaus.org/dbl/">...</a>
   */
  DBL_SPAMHAUS_ORG("dbl.spamhaus.org", DnsBlockListType.DOMAIN, Set.of() );


  private final String dnsBlockListZone;
  private final Set<String> blockingKnownResponses;
  private final DnsBlockListType blockListQueryType;

  /**
   * @param dnsZone                - the dns zone
   * @param blockListType          - the type of block list
   * @param blockingKnownResponses - the response known to be blocking (any other are errors)
   */
  DnsBlockList(String dnsZone, DnsBlockListType blockListType, Set<String> blockingKnownResponses) {
    this.dnsBlockListZone = dnsZone;
    this.blockingKnownResponses = blockingKnownResponses;
    this.blockListQueryType = blockListType;
  }

  public String getZone() {
    return dnsBlockListZone;
  }

  public Set<String> getBlockingKnownResponses() {
    return this.blockingKnownResponses;
  }

  @Override
  public String toString() {
    return dnsBlockListZone;
  }

  public DnsBlockListType getType() {
    return this.blockListQueryType;
  }
}
