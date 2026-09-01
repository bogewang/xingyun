package com.lframework.xingyun.basedata.converter.quote;

import com.lframework.xingyun.basedata.bo.quote.GetQuoteSheetBo;
import com.lframework.xingyun.basedata.bo.quote.QueryQuoteSheetBo;
import com.lframework.xingyun.basedata.bo.quote.QuoteProductBo;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheet;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheetDetail;
import com.lframework.xingyun.basedata.vo.quote.CreateQuoteSheetVo;
import com.lframework.xingyun.basedata.vo.quote.QuoteSheetProductVo;
import java.util.List;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** 报价单对象 MapStruct 转换器。 */
@Mapper(componentModel = "spring")
public interface QuoteSheetConverter {
  /** 将新增请求转换为报价单实体。 */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  QuoteSheet toEntity(CreateQuoteSheetVo source);
  /** 将报价单转换为列表响应。 */
  QueryQuoteSheetBo toQueryBo(QuoteSheet source);
  /** 将报价单转换为详情响应。 */
  GetQuoteSheetBo toGetBo(QuoteSheet source);
  /** 将商品请求转换为报价明细。 */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "quoteSheetId", source = "quoteSheetId")
  QuoteSheetDetail toDetail(QuoteSheetProductVo source, String quoteSheetId);
  /** 将报价明细转换为商品响应。 */
  @Mapping(target = "sourceId", source = "id")
  QuoteProductBo toProductBo(QuoteSheetDetail source);
  /** 将商品基础展示字段和报价业务上下文转换为报价商品响应。 */
  @Mapping(target = "productId", source = "product.id")
  @Mapping(target = "quoteSheetId", source = "quoteSheetId")
  @Mapping(target = "salePrice", source = "salePrice")
  QuoteProductBo toProductBo(Product product, BigDecimal salePrice, String quoteSheetId);
  /** 将报价明细列表转换为商品响应列表。 */
  List<QuoteProductBo> toProductBos(List<QuoteSheetDetail> source);
}
