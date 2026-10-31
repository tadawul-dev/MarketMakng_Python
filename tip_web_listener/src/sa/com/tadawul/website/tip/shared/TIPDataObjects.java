package sa.com.tadawul.website.tip.shared;

import java.util.HashMap;
import java.util.Map;

import sa.com.tadawul.website.tip.vo.TIP_BondIndexVO;
import sa.com.tadawul.website.tip.vo.TIP_BondVO;
import sa.com.tadawul.website.tip.vo.TIP_IndexVO;
import sa.com.tadawul.website.tip.vo.TIP_MarketVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEIndexVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEShareVO;
import sa.com.tadawul.website.tip.vo.TIP_ShareVO;

public class TIPDataObjects {
	
		private static final float LOADFACTOR=0.75f;
	
		public volatile static TIP_IndexVO[] tipIndex = new TIP_IndexVO[TipConstants.TIP_MAX_NO_INDICES];
	    public volatile  static TIP_BondIndexVO[] tipBondIndex = new TIP_BondIndexVO[TipConstants.TIP_MAX_NO_BOND_INDICES];
	    public volatile static TIP_SMEIndexVO[] tipSMEIndex = new TIP_SMEIndexVO[TipConstants.TIP_MAX_NO_SME_INDICES];
	   	    
	    public volatile static TIP_ShareVO[] tipShare = new TIP_ShareVO[TipConstants.TIP_MAX_NO_SHARES];
	    public volatile static TIP_SMEShareVO[] tipSMEShare = new TIP_SMEShareVO[TipConstants.TIP_MAX_NO_SME_SHARES];
	    public volatile static TIP_MarketVO[] tipMarket = new TIP_MarketVO[TipConstants.TIP_MAX_NO_MARKET];
	    public volatile static TIP_BondVO[] tipBond = new TIP_BondVO[TipConstants.TIP_MAX_NO_BONDS];
	    
	    //Added for PTTP Project CRQ22398
	    public volatile static TIP_ShareVO[] tipOTCShare = new TIP_ShareVO[TipConstants.TIP_MAX_NO_OTC_SHARES];
	    public volatile static TIP_BondVO[] tipOTCBond = new TIP_BondVO[TipConstants.TIP_MAX_NO_BONDS];
	    
	    public volatile static Map<String,Integer> tipIndex_Symbol_ArrayIndexMap=new HashMap<String,Integer>((int) Math.ceil (TipConstants.TIP_MAX_NO_INDICES/LOADFACTOR +1) );
	    public volatile static Map<String,Integer> tipBondIndex_Symbol_ArrayIndexMap=new HashMap<String,Integer>( (int) Math.ceil (TipConstants.TIP_MAX_NO_BOND_INDICES/LOADFACTOR +1));
	    public volatile static Map<String,Integer> tipSMEIndex_Symbol_ArrayIndexMap=new HashMap<String,Integer>((int) Math.ceil (TipConstants.TIP_MAX_NO_SME_INDICES/LOADFACTOR +1) );
	    public volatile static Map<String,Integer> tipShare_Symbol_ArrayIndexMap=new HashMap<String,Integer>( (int) Math.ceil (TipConstants.TIP_MAX_NO_SHARES/LOADFACTOR +1));
	    public volatile static Map<String,Integer> tipSMEShare_Symbol_ArrayIndexMap=new HashMap<String,Integer>((int) Math.ceil (TipConstants.TIP_MAX_NO_SME_SHARES/LOADFACTOR +1) );
	    public volatile static Map<String,Integer> tipMarket_Symbol_ArrayIndexMap=new HashMap<String,Integer>();
	    public volatile static Map<String,Integer> tipBond_Symbol_ArrayIndexMap=new HashMap<String,Integer>((int) Math.ceil (TipConstants.TIP_MAX_NO_BONDS/LOADFACTOR +1) );
	    
	    //Tohave Index Id vs sysmbols
	    public volatile static Map<Integer,String> tipIndexConsolidated_IdCodeToSymbolMap=new HashMap<Integer,String>((int) Math.ceil (TipConstants.TIP_MAX_NO_INDICES+TipConstants.TIP_MAX_NO_SME_INDICES/LOADFACTOR +1) );
	  
	    
	    //Added for PTTP Project CRQ22398
	    public volatile static Map<String,Integer> tipOTCShare_Symbol_ArrayIndexMap=new HashMap<String,Integer>( (int) Math.ceil (TipConstants.TIP_MAX_NO_OTC_SHARES/LOADFACTOR +1));
	    public volatile static Map<String,Integer> tipOTCBond_Symbol_ArrayIndexMap=new HashMap<String,Integer>( (int) Math.ceil (TipConstants.TIP_MAX_NO_OTC_SHARES/LOADFACTOR +1));
	    
	    public volatile static boolean isShareBODOpen = false;
	    public volatile static boolean isSMEShareBODOpen = false;
	    public volatile static boolean isBondBODOpen = false;
	    
	    //Added for Market Data Initialization CRQ24298
	    public volatile static boolean isShareBODPreOpen = false;
	    public volatile static boolean isSMEShareBODPreOpen = false;
	    public volatile static boolean isBondBODPreOpen = false;
	    
	    public volatile static boolean isShareEOD = false;
	    public volatile static boolean isSMEShareEOD = false;
	    public volatile static boolean isBondEOD = false;
	    
	    public volatile static int totalMessagesParsed=0;
	    
	    
	    public volatile static int totIndexUpdated = 0;
	    public volatile static int totSMEIndexUpdated = 0;
	    public volatile static int totBondIndexUpdated = 0;
	    public volatile static int totShareUpdated = 0;
	    public volatile static int totSMEShareUpdated = 0;
	    public volatile static int totBondUpdated = 0;
	    public volatile static int totMarketUpdated = 0;
	    
	    
		public volatile static int   tipShareIter=0;
		public volatile static int   tipShareSMEIter=0;
		public volatile static int   tipBondIter=0;
		public volatile static int   tipIndexIter=0;
		public volatile static int   tipSMEIndexIter=0;
		public volatile static int   tipBondIndexIter=0;
		
		 //Added for PTTP Project CRQ22398
		public volatile static int   tipOTCShareIter=0;
		public volatile static int totOTCShareUpdated = 0;
		
		 //Added for PTTP Project CRQ22398
		public volatile static int   tipOTCBonditer=0;
		public volatile static int totOTCBondUpdated = 0;
	    
	    public volatile static boolean  replayMode = false;
	    
	    public volatile static Map<String,String> SharedList_Mapping_TLU=new HashMap<String,String>(3);
	    
	    public volatile static long last_Sequencer_Analyzer=0;
	    
	    //For NegDeals changes
	    public volatile static boolean isAfterPreOpen = false;
	    
	    
	    
	    
	    
}
