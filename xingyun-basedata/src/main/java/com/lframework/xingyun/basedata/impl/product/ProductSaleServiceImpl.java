package com.lframework.xingyun.basedata.impl.product;

import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.xingyun.basedata.entity.ProductSale;
import com.lframework.xingyun.basedata.enums.BaseDataOpLogType;
import com.lframework.xingyun.basedata.mappers.ProductSaleMapper;
import com.lframework.xingyun.basedata.service.product.ProductSaleService;
import com.lframework.xingyun.basedata.vo.product.sale.CreateProductSaleVo;
import com.lframework.xingyun.basedata.vo.product.sale.UpdateProductSaleVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductSaleServiceImpl extends BaseMpServiceImpl<ProductSaleMapper, ProductSale>
		implements ProductSaleService {

	@OpLog(type = BaseDataOpLogType.class, name = "设置商品销售价，ID：{}, 销售价：{}", params = { "#vo.id", "#vo.price" })
	@Transactional(rollbackFor = Exception.class)
	@Override
	public String create(CreateProductSaleVo vo) {

		ProductSale data = getProductSale(vo);

		getBaseMapper().insert(data);

		return data.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void batchCreate(List<CreateProductSaleVo> vos) {
		if (vos == null || vos.isEmpty()) {
			return;
		}
		List<ProductSale> productSales = vos.stream()
				.map(ProductSaleServiceImpl::getProductSale)
				.collect(Collectors.toList());
		saveBatch(productSales);
	}

	private static ProductSale getProductSale(CreateProductSaleVo vo) {
		ProductSale data = new ProductSale();
		data.setId(IdUtil.getId());
		if (!StringUtil.isBlank(vo.getId())) {
			data.setId(vo.getId());
		}

		data.setPrice(vo.getPrice());
		return data;
	}

	@OpLog(type = BaseDataOpLogType.class, name = "设置商品销售价，ID：{}, 销售价：{}", params = { "#vo.id", "#vo.price" })
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void update(UpdateProductSaleVo vo) {

		if (vo.getPrice() == null) {
			throw new InputErrorException("销售价不能为空！");
		}

		if (vo.getPrice().doubleValue() < 0D) {
			throw new InputErrorException("销售价必须大于0！");
		}

		getBaseMapper().deleteById(vo.getId());

		CreateProductSaleVo createVo = new CreateProductSaleVo();
		createVo.setId(vo.getId());
		createVo.setPrice(vo.getPrice());

		this.create(createVo);
	}
}
