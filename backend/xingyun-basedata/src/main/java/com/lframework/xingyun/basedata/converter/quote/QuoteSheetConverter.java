package com.lframework.xingyun.basedata.converter.quote;
import com.lframework.xingyun.basedata.bo.quote.*; import com.lframework.xingyun.basedata.entity.quote.*; import com.lframework.xingyun.basedata.vo.quote.*; import java.util.*; import java.util.stream.Collectors; import org.springframework.beans.BeanUtils;
/** 报价单对象转换器。 */ public final class QuoteSheetConverter { private QuoteSheetConverter() { }
 /** 将新增请求转换为实体。 */ public static QuoteSheet toEntity(CreateQuoteSheetVo source) { QuoteSheet target=new QuoteSheet(); BeanUtils.copyProperties(source,target); return target; }
 /** 将报价单转换为列表响应。 */ public static QueryQuoteSheetBo toQueryBo(QuoteSheet source) { QueryQuoteSheetBo target=new QueryQuoteSheetBo(); BeanUtils.copyProperties(source,target); return target; }
 /** 将报价单转换为详情响应。 */ public static GetQuoteSheetBo toGetBo(QuoteSheet source,List<QuoteProductBo> products) { GetQuoteSheetBo target=new GetQuoteSheetBo(); BeanUtils.copyProperties(source,target); target.setProducts(products); return target; }
 /** 将商品请求转换为报价明细。 */ public static QuoteSheetDetail toDetail(QuoteSheetProductVo source,String quoteSheetId) { QuoteSheetDetail target=new QuoteSheetDetail(); target.setQuoteSheetId(quoteSheetId); target.setProductId(source.getProductId()); target.setSalePrice(source.getSalePrice()); return target; }
 /** 将明细列表转换为商品响应列表。 */ public static List<QuoteProductBo> toProductBos(List<QuoteSheetDetail> source) { return source.stream().map(detail->{ QuoteProductBo bo=new QuoteProductBo(); bo.setQuoteSheetId(detail.getQuoteSheetId()); bo.setProductId(detail.getProductId()); bo.setSalePrice(detail.getSalePrice()); return bo; }).collect(Collectors.toList()); }
}
