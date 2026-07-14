function Pagination({ page, setPage, totalPages }) {
  return (
    <div
      style={{
        display: "flex",
        gap: "10px",
        marginTop: "20px",
      }}
    >
      <button disabled={page === 1} onClick={() => setPage(page - 1)}>
        Previous
      </button>

      <span>
        Page {page} of {totalPages}
      </span>

      <button disabled={page === totalPages} onClick={() => setPage(page + 1)}>
        Next
      </button>
    </div>
  );
}

export default Pagination;
