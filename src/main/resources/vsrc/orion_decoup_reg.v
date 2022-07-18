`timescale 1ns/1ps
module orion_decoup_reg #(
  parameter   WIDTH     = 1,
  parameter   D_INIT    = 0,
  parameter   PI_INIT   = 0,
  parameter   PO_INIT   = 0
)(
  input  wire             reset,
  output wire             in_ack,
  input  wire             in_req,
  input  wire [WIDTH-1:0] in_data,
  
  input  wire             out_ack,
  output wire             out_req,
  output reg  [WIDTH-1:0] out_data  
);

reg pi;
reg po;

wire click;
wire click_del;

assign #10ps click = (in_req ^ pi) & ~(out_ack ^ po);

assign #10ps click_del = click;

assign in_ack  = pi;
assign out_req = po;

always @(posedge click_del or posedge reset) begin
  if(reset) begin
    pi          <= PI_INIT;
    po          <= PO_INIT;
    out_data    <= D_INIT;
  end else begin
    pi          <= #10ps ~pi;
    po          <= #10ps ~po;
    out_data    <= #10ps in_data;
  end
end


endmodule
