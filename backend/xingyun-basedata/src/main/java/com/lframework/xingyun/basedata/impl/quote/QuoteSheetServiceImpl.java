package com.lframework.xingyun.basedata.impl.quote;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.bo.quote.*;
import com.lframework.xingyun.basedata.converter.quote.QuoteSheetConverter;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.quote.*;
import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus;
import com.lframework.xingyun.basedata.mappers.ProductMapper;
import com.lframework.xingyun.basedata.mappers.quote.*;
import com.lframework.xingyun.basedata.service.quote.*;
import com.lframework.xingyun.basedata.vo.quote.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 报价单服务实现。 */
@Service
public class QuoteSheetServiceImpl extends BaseMpServiceImpl<QuoteSheetMapper, QuoteSheet> implements QuoteSheetService {
  @Autowired private QuoteSheetDetailMapper quoteSheetDetailMapper;
  @Autowired private ProductMapper productMapper;
  @Autowired(required = false) private List<QuoteSheetReferenceChecker> quoteSheetReferenceCheckers = Collections.emptyList();

  /** 创建报价单。 */
  @Override @Transactional(rollbackFor = Exception.class)
  public String create(CreateQuoteSheetVo vo) {
    validateSave(vo, null);
    QuoteSheet sheet = QuoteSheetConverter.toEntity(vo); sheet.setId(IdUtil.getId()); sheet.setStatus(QuoteSheetStatus.DISABLED);
    getBaseMapper().insert(sheet); saveDetails(sheet.getId(), vo.getProducts()); return sheet.getId();
  }
  /** 更新报价单及其明细。 */
  @Override @Transactional(rollbackFor = Exception.class)
  public void update(UpdateQuoteSheetVo vo) {
    QuoteSheet existed = requireSheet(vo.getId()); validateSave(vo, vo.getId());
    QuoteSheet sheet = QuoteSheetConverter.toEntity(vo); sheet.setId(existed.getId()); sheet.setStatus(existed.getStatus());
    getBaseMapper().updateById(sheet);
    quoteSheetDetailMapper.delete(Wrappers.lambdaQuery(QuoteSheetDetail.class).eq(QuoteSheetDetail::getQuoteSheetId, vo.getId()));
    saveDetails(vo.getId(), vo.getProducts());
  }
  /** 删除未被业务引用的报价单。 */
  @Override @Transactional(rollbackFor = Exception.class)
  public void deleteById(String id) {
    requireSheet(id); if (quoteSheetReferenceCheckers.stream().anyMatch(checker -> checker.hasReference(id))) throw new DefaultClientException("报价单已被销售单使用，不能删除！");
    quoteSheetDetailMapper.delete(Wrappers.lambdaQuery(QuoteSheetDetail.class).eq(QuoteSheetDetail::getQuoteSheetId, id)); getBaseMapper().deleteById(id);
  }
  /** 启用报价单并复核定价周期。 */
  @Override @Transactional(rollbackFor = Exception.class)
  public void enable(String id) { QuoteSheet sheet = requireSheet(id); assertNoDateRangeOverlap(id, sheet.getStartDate(), sheet.getEndDate(), getBaseMapper().selectList(Wrappers.lambdaQuery(QuoteSheet.class))); sheet.setStatus(QuoteSheetStatus.ENABLED); getBaseMapper().updateById(sheet); }
  /** 停用报价单。 */
  @Override @Transactional(rollbackFor = Exception.class)
  public void disable(String id) { QuoteSheet sheet = requireSheet(id); sheet.setStatus(QuoteSheetStatus.DISABLED); getBaseMapper().updateById(sheet); }
  /** 获取报价单详情及商品基础展示字段。 */
  @Override public GetQuoteSheetBo get(String id) {
    QuoteSheet sheet = requireSheet(id); List<QuoteSheetDetail> details=quoteSheetDetailMapper.selectList(Wrappers.lambdaQuery(QuoteSheetDetail.class).eq(QuoteSheetDetail::getQuoteSheetId,id));
    Map<String,Product> products=details.isEmpty()?Collections.emptyMap():productMapper.selectList(Wrappers.lambdaQuery(Product.class).in(Product::getId,details.stream().map(QuoteSheetDetail::getProductId).collect(Collectors.toSet()))).stream().collect(Collectors.toMap(Product::getId,p->p));
    List<QuoteProductBo> result=QuoteSheetConverter.toProductBos(details); result.forEach(bo->{Product p=products.get(bo.getProductId()); if(p!=null){bo.setCode(p.getCode());bo.setName(p.getName());bo.setShortName(p.getShortName());bo.setSkuCode(p.getSkuCode());bo.setSpec(p.getSpec());bo.setUnit(p.getUnit());}}); return QuoteSheetConverter.toGetBo(sheet,result);
  }
  /** 分页查询报价单。 */
  @Override public PageResult<QuoteSheet> query(Integer pageIndex,Integer pageSize,QueryQuoteSheetVo vo) { PageHelperUtil.startPage(pageIndex,pageSize); return PageResultUtil.convert(new PageInfo<>(getBaseMapper().selectList(Wrappers.lambdaQuery(QuoteSheet.class).eq(vo.getStatus()!=null,QuoteSheet::getStatus,vo.getStatus()).eq(vo.getCode()!=null,QuoteSheet::getCode,vo.getCode()).like(vo.getName()!=null,QuoteSheet::getName,vo.getName()).ge(vo.getStartDate()!=null,QuoteSheet::getStartDate,vo.getStartDate()).le(vo.getEndDate()!=null,QuoteSheet::getEndDate,vo.getEndDate()).orderByDesc(QuoteSheet::getCreateTime)))); }
  /** 一次关联查询指定日期的已启用报价商品。 */
  @Override public List<QuoteProductBo> getActiveQuoteProducts(QueryQuoteProductVo vo) { return getBaseMapper().getActiveQuoteProducts(vo); }
  /** 校验保存请求。 */
  private void validateSave(CreateQuoteSheetVo vo,String excludeId) { if(vo.getStartDate().isAfter(vo.getEndDate())) throw new DefaultClientException("生效开始日期不能晚于生效结束日期！"); assertNoDuplicatedProducts(vo.getProducts()); assertNoDateRangeOverlap(excludeId,vo.getStartDate(),vo.getEndDate(),getBaseMapper().selectList(Wrappers.lambdaQuery(QuoteSheet.class))); }
  /** 批量保存报价单明细。 */
  private void saveDetails(String quoteSheetId,List<QuoteSheetProductVo> products) { List<QuoteSheetDetail> details=products.stream().map(p->{QuoteSheetDetail d=QuoteSheetConverter.toDetail(p,quoteSheetId);d.setId(IdUtil.getId());return d;}).collect(Collectors.toList()); quoteSheetDetailMapper.batchInsert(details); }
  /** 获取不存在时报业务异常。 */
  private QuoteSheet requireSheet(String id) { QuoteSheet result=getBaseMapper().selectById(id); if(result==null) throw new DefaultClientException("报价单不存在！"); return result; }
  /** 判断两个日期闭区间是否重叠。 */
  public static boolean isDateRangeOverlapped(LocalDate startA,LocalDate endA,LocalDate startB,LocalDate endB) { return !endA.isBefore(startB)&&!startA.isAfter(endB); }
  /** 校验报价周期不与其他未删除报价单重叠。 */
  public static void assertNoDateRangeOverlap(String excludeId,LocalDate startDate,LocalDate endDate,List<QuoteSheet> sheets) { if(sheets.stream().filter(s->!Objects.equals(s.getId(),excludeId)).anyMatch(s->isDateRangeOverlapped(startDate,endDate,s.getStartDate(),s.getEndDate()))) throw new DefaultClientException("定价周期与已有报价单冲突！"); }
  /** 校验同一报价单内商品不重复。 */
  public static void assertNoDuplicatedProducts(List<QuoteSheetProductVo> products) { if(products.stream().map(QuoteSheetProductVo::getProductId).collect(Collectors.toSet()).size()!=products.size()) throw new DefaultClientException("同一报价单商品不能重复！"); }
  /** 判断报价单是否在指定日期生效。 */
  public static boolean isActiveOn(QuoteSheet sheet,LocalDate orderDate) { return sheet.getStatus()==QuoteSheetStatus.ENABLED && !orderDate.isBefore(sheet.getStartDate()) && !orderDate.isAfter(sheet.getEndDate()); }
}
